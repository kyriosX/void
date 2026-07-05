package com.kirioslab.digestbot.telegram;

import com.kirioslab.digestbot.model.NewsItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelegramMessageBuilderTest {

    @Test
    void groupsItemsByTopicInEncounterOrderWithinOneMessage() {
        List<NewsItem> items = List.of(
                new NewsItem("T1", "https://example.com/1", "Java", "why1"),
                new NewsItem("T2", "https://example.com/2", "Angular", "why2"),
                new NewsItem("T3", "https://example.com/3", "Java", "why3"));
        List<String> messages = TelegramMessageBuilder.buildMessages(items, 10_000);
        assertEquals(1, messages.size());
        String msg = messages.get(0);
        assertTrue(msg.indexOf("Java") < msg.indexOf("Angular"));
        assertTrue(msg.contains("T1") && msg.contains("T2") && msg.contains("T3"));
    }

    @Test
    void splitsIntoMultipleMessagesWhenOverCharLimit() {
        List<NewsItem> items = List.of(
                new NewsItem("T1", "https://example.com/1", "Java", "why1"),
                new NewsItem("T2", "https://example.com/2", "Angular", "why2"));
        List<String> messages = TelegramMessageBuilder.buildMessages(items, 60);
        assertTrue(messages.size() > 1);
        for (String m : messages) assertTrue(m.length() <= 60 || m.contains("T1") || m.contains("T2"));
    }

    @Test
    void blankWhyOmitsSuffix() {
        List<NewsItem> items = List.of(new NewsItem("T1", "https://example.com/1", "Java", ""));
        String msg = TelegramMessageBuilder.buildMessages(items, 10_000).get(0);
        assertFalse(msg.contains("T1</a> — "));
    }

    @Test
    void nonBlankWhyIncludesSuffix() {
        List<NewsItem> items = List.of(new NewsItem("T1", "https://example.com/1", "Java", "because reasons"));
        String msg = TelegramMessageBuilder.buildMessages(items, 10_000).get(0);
        assertTrue(msg.contains(" — because reasons"));
    }

    @Test
    void escapeHtmlEscapesReservedCharacters() {
        assertEquals("a &amp; b &lt;tag&gt;", TelegramMessageBuilder.escapeHtml("a & b <tag>"));
    }

    @Test
    void emptyItemsListReturnsSingleHeaderOnlyMessage() {
        List<String> messages = TelegramMessageBuilder.buildMessages(List.of(), 10_000);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("Dev digest"));
    }
}
