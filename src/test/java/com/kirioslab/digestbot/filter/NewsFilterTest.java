package com.kirioslab.digestbot.filter;

import com.kirioslab.digestbot.model.NewsItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsFilterTest {

    private static NewsItem item(String url) {
        return new NewsItem("title-" + url, url, "Java", "why");
    }

    @Test
    void dropsAlreadySeenUrl() {
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(item("https://example.com/a")),
                Set.of("example.com/a"),
                Set.of(),
                15, false);
        assertTrue(result.isEmpty());
    }

    @Test
    void dropsWithinBatchDuplicateKeepingFirst() {
        NewsItem first = item("https://example.com/a");
        NewsItem duplicate = item("https://www.example.com/a/");
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(first, duplicate), Set.of(), Set.of(), 15, false);
        assertEquals(List.of(first), result);
    }

    @Test
    void groundingCheckDropsUngroundedItemsWhenSearchUrlsNonEmpty() {
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(item("https://example.com/a"), item("https://example.com/b")),
                Set.of(),
                Set.of("example.com/a"),
                15, true);
        assertEquals(List.of(item("https://example.com/a")), result);
    }

    @Test
    void groundingCheckSkippedWhenSearchUrlsEmpty() {
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(item("https://example.com/a")),
                Set.of(),
                Set.of(),
                15, true);
        assertEquals(1, result.size());
    }

    @Test
    void groundingCheckFalseKeepsUngroundedItems() {
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(item("https://example.com/a")),
                Set.of(),
                Set.of("example.com/other"),
                15, false);
        assertEquals(1, result.size());
    }

    @Test
    void capsResultAtMaxItems() {
        List<NewsItem> candidates = List.of(
                item("https://example.com/1"), item("https://example.com/2"), item("https://example.com/3"));
        List<NewsItem> result = NewsFilter.filterFresh(candidates, Set.of(), Set.of(), 2, false);
        assertEquals(2, result.size());
    }

    @Test
    void dropsBlankUrl() {
        List<NewsItem> result = NewsFilter.filterFresh(
                List.of(new NewsItem("t", "", "Java", "")), Set.of(), Set.of(), 15, false);
        assertTrue(result.isEmpty());
    }
}
