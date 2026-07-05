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
                Developer news digest. Today: %s.
                Run at most 5–10 web searches for items published in the LAST 48 HOURS on Java, Angular, and Spring Boot: 
                releases, advanced deep-dives, emerging best practices, highly cited items, emerging reddit topics. Exclude tutorials, listicles, SEO spam, AI junk.
                
                Judge relevance from search result titles + snippets ONLY. Do NOT open/fetch pages. 
                Take url and title verbatim from results. If a snippet is too thin to judge, skip it. Max 8 items total.
                
                Exclude anything already covered:
                %s
                
                Output ONLY a JSON array (no prose, no code fences, no reasoning). Each item:
                {"title","url","topic":"Java"|"Angular"|"Spring Boot","why":"≤30 words"}
                Nothing qualifies → [].
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
