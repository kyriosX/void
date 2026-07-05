package com.kirioslab.digestbot.claude;

import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.DirectCaller;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingBlock;
import com.anthropic.models.messages.WebSearchResultBlock;
import com.anthropic.models.messages.WebSearchToolResultBlock;
import com.anthropic.models.messages.WebSearchToolResultBlockContent;
import com.kirioslab.digestbot.model.NewsItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeResponseParserTest {

    private static ContentBlock text(String text) {
        return ContentBlock.ofText(TextBlock.builder().text(text).citations(Optional.empty()).build());
    }

    private static ContentBlock webSearchResult(String... urls) {
        List<WebSearchResultBlock> results = List.of(urls).stream()
                .map(u -> WebSearchResultBlock.builder().url(u).title("t").encryptedContent("enc").pageAge(Optional.empty()).build())
                .toList();
        return ContentBlock.ofWebSearchToolResult(WebSearchToolResultBlock.builder()
                .toolUseId("tool_1")
                .caller(DirectCaller.builder().build())
                .content(WebSearchToolResultBlockContent.ofResultBlocks(results))
                .build());
    }

    @Test
    void extractTextConcatenatesTextBlocksAndIgnoresOthers() {
        List<ContentBlock> content = List.of(
                text("hello "),
                ContentBlock.ofThinking(ThinkingBlock.builder().thinking("ignored").signature("sig").build()),
                text("world"));
        assertEquals("hello world", ClaudeResponseParser.extractText(content));
    }

    @Test
    void extractTextOnEmptyContentReturnsEmptyString() {
        assertEquals("", ClaudeResponseParser.extractText(List.of()));
    }

    @Test
    void extractSearchUrlsFindsResultUrlsAndDedupesViaNormalization() {
        List<ContentBlock> content = List.of(webSearchResult(
                "https://www.example.com/a/", "http://example.com/a", "https://example.com/b"));
        Set<String> urls = ClaudeResponseParser.extractSearchUrls(content);
        assertEquals(Set.of("example.com/a", "example.com/b"), urls);
    }

    @Test
    void extractSearchUrlsOnNoUrlsReturnsEmptySet() {
        assertTrue(ClaudeResponseParser.extractSearchUrls(List.of(text("no urls here"))).isEmpty());
    }

    @Test
    void parseItemsHandlesWellFormedArray() {
        String json = """
                [{"title": "T1", "url": "https://example.com/1", "topic": "Java", "why": "because"}]
                """;
        List<NewsItem> items = ClaudeResponseParser.parseItems(json);
        assertEquals(1, items.size());
        assertEquals(new NewsItem("T1", "https://example.com/1", "Java", "because"), items.get(0));
    }

    @Test
    void parseItemsSkipsEntriesMissingUrlOrTitle() {
        String json = """
                [
                    {"title": "", "url": "https://example.com/1"},
                    {"title": "T2", "url": ""},
                    {"title": "T3", "url": "https://example.com/3"}
                ]
                """;
        List<NewsItem> items = ClaudeResponseParser.parseItems(json);
        assertEquals(1, items.size());
        assertEquals("T3", items.get(0).title());
    }

    @Test
    void parseItemsTolerantOfSurroundingProseAndCodeFences() {
        String text = "Here you go:\n```json\n[{\"title\": \"T\", \"url\": \"https://example.com\"}]\n```\nThanks!";
        List<NewsItem> items = ClaudeResponseParser.parseItems(text);
        assertEquals(1, items.size());
    }

    @Test
    void parseItemsOnMalformedJsonReturnsEmptyListWithoutThrowing() {
        String text = "[{\"title\": \"T\", \"url\": }]";
        assertEquals(List.of(), ClaudeResponseParser.parseItems(text));
    }

    @Test
    void parseItemsOnEmptyArrayReturnsEmptyList() {
        assertEquals(List.of(), ClaudeResponseParser.parseItems("[]"));
    }

    @Test
    void systemPromptEmbedsDateAndRecentList() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        String prompt = ClaudeResponseParser.systemPrompt("- Old item (https://example.com)\n", today);
        assertTrue(prompt.contains("2026-01-15"));
        assertTrue(prompt.contains("- Old item (https://example.com)"));
    }

    @Test
    void systemPromptOnEmptyRecentListUsesFallback() {
        String prompt = ClaudeResponseParser.systemPrompt("", LocalDate.of(2026, 1, 15));
        assertTrue(prompt.contains("(nothing yet)"));
    }
}
