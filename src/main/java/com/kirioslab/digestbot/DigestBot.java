package com.kirioslab.digestbot;

import com.anthropic.models.messages.Message;
import com.kirioslab.digestbot.claude.ClaudeClient;
import com.kirioslab.digestbot.claude.ClaudeResponseParser;
import com.kirioslab.digestbot.filter.NewsFilter;
import com.kirioslab.digestbot.model.NewsItem;
import com.kirioslab.digestbot.model.SeenItem;
import com.kirioslab.digestbot.seen.SeenStore;
import com.kirioslab.digestbot.telegram.TelegramClient;
import com.kirioslab.digestbot.telegram.TelegramMessageBuilder;
import com.kirioslab.digestbot.util.UrlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One-shot digest job. Run once per day by GitHub Actions:
 *   1. read resources/seen.json  (the git-committed "database")
 *   2. ask Claude for fresh news  (web search tool does the discovery)
 *   3. keep only new, grounded links
 *   4. post them to a Telegram channel
 *   5. rewrite resources/seen.json (the workflow commits it back)
 *
 * A failure just means nothing is posted today, seen.json is untouched,
 * and the run shows red in Actions — tomorrow's run retries cleanly.
 */
public class DigestBot {

    private static final Logger log = LoggerFactory.getLogger(DigestBot.class);

    public static void main(String[] args) throws Exception {
        log.info("=== Daily dev digest starting ===");

        Config config = Config.fromEnv();
        log.info("Config loaded: model={}, maxItems={}, groundingCheck={}", config.model(), config.maxItems(), config.groundingCheck());

        SeenStore seenStore = new SeenStore();
        ClaudeClient claude = new ClaudeClient(config.anthropicApiKey(), config.model());
        TelegramClient telegram = new TelegramClient(config.telegramBotToken(), config.telegramChannelId());

        List<SeenItem> seen = seenStore.load();
        var seenUrls = seen.stream().map(s -> UrlNormalizer.normalize(s.url())).collect(Collectors.toSet());

        // 1. Ask Claude (it runs the searches server-side and returns a JSON array).
        Message response = claude.ask(SeenStore.recentSlice(seen, config.recentWindow()));

        // 2. Pull Claude's items out of the text, and the real result URLs out of the structure.
        List<NewsItem> candidates = ClaudeResponseParser.parseItems(ClaudeResponseParser.extractText(response.content()));
        var searchUrls = ClaudeResponseParser.extractSearchUrls(response.content());
        log.info("Parsed {} candidate item(s) from Claude's response ({} grounded search result URL(s))", candidates.size(), searchUrls.size());

        // 3. Filter: not already sent, not duplicated within the batch, and (optionally) grounded.
        List<NewsItem> fresh = NewsFilter.filterFresh(candidates, seenUrls, searchUrls, config.maxItems(), config.groundingCheck());

        if (fresh.isEmpty()) {
            log.info("No new items today — nothing to post.");
            return; // seen.json unchanged, so the workflow makes no commit
        }

        // 4. Post to Telegram (split into multiple messages if it's long).
        List<String> messages = TelegramMessageBuilder.buildMessages(fresh, config.tgLimit());
        log.info("Posting {} item(s) to Telegram across {} message(s)", fresh.size(), messages.size());
        for (String message : messages) telegram.send(message);

        // 5. Update the "database" — the workflow commits this file back to the repo.
        String today = LocalDate.now().toString();
        for (NewsItem it : fresh) seen.add(new SeenItem(it.url(), it.title(), today));
        seenStore.save(seen);
        log.info("=== Done — posted {} item(s) ===", fresh.size());
    }
}
