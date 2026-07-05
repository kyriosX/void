package com.kirioslab.digestbot.claude;

import com.anthropic.models.messages.ContentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirioslab.digestbot.model.NewsItem;
import com.kirioslab.digestbot.util.UrlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure parsing/formatting over Claude's HTTP response — no network I/O. */
public final class ClaudeResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ClaudeResponseParser.class);
    private static final ObjectMapper M = new ObjectMapper();

    private ClaudeResponseParser() {}

    public static String systemPrompt(String recentList) {
        return systemPrompt(recentList, LocalDate.now());
    }

    public static String systemPrompt(String recentList, LocalDate today) {
        String avoid = recentList.isBlank() ? "(nothing yet)" : recentList;
        return """
                You curate a developer news digest. Today is %s.
                Search the web for the most valuable things published in the LAST 48 HOURS \
                across Java, Angular, and Spring Boot: new releases, security advisories, \
                notable deep-dive articles, and major ecosystem news. Prefer official and \
                primary sources. Skip beginner tutorials, listicles and SEO spam.

                Do NOT include anything already covered recently:
                %s

                Return ONLY a JSON array, with no prose and no code fences. Each element:
                {"title": "...", "url": "...", "topic": "Java" | "Angular" | "Spring Boot", "why": "one sentence"}
                Every url must be a real page you found in the search results. If nothing new qualifies, return [].
                """.formatted(today, avoid);
    }

    /** All text blocks concatenated — this is where Claude's JSON array lives. */
    public static String extractText(List<ContentBlock> content) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : content) block.text().ifPresent(t -> sb.append(t.text()));
        return sb.toString();
    }

    /** URLs from every web-search result block = the real pages search actually returned. */
    public static Set<String> extractSearchUrls(List<ContentBlock> content) {
        Set<String> urls = new HashSet<>();
        for (ContentBlock block : content) {
            block.webSearchToolResult().flatMap(result -> result.content().resultBlocks()).ifPresent(results ->
                    results.forEach(r -> urls.add(UrlNormalizer.normalize(r.url()))));
        }
        return urls;
    }

    public static List<NewsItem> parseItems(String text) {
        String t = text.trim();
        int start = t.indexOf('['), end = t.lastIndexOf(']'); // tolerate stray prose / fences
        if (start < 0 || end <= start) return List.of();
        try {
            List<NewsItem> out = new ArrayList<>();
            for (JsonNode n : M.readTree(t.substring(start, end + 1))) {
                String url = n.path("url").asText(""), title = n.path("title").asText("");
                if (url.isBlank() || title.isBlank()) continue;
                out.add(new NewsItem(title, url, n.path("topic").asText("General"), n.path("why").asText("")));
            }
            return out;
        } catch (Exception e) {
            log.warn("Could not parse Claude's JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
