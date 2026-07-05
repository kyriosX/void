package com.kirioslab.digestbot.filter;

import com.kirioslab.digestbot.model.NewsItem;
import com.kirioslab.digestbot.util.UrlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Keeps only new, non-duplicate, (optionally) grounded items — pure, no I/O. */
public final class NewsFilter {

    private static final Logger log = LoggerFactory.getLogger(NewsFilter.class);

    private NewsFilter() {}

    public static List<NewsItem> filterFresh(
            List<NewsItem> candidates,
            Set<String> seenUrls,
            Set<String> searchUrls,
            int maxItems,
            boolean groundingCheck
    ) {
        List<NewsItem> fresh = new ArrayList<>();
        Set<String> batchUrls = new HashSet<>();
        for (NewsItem it : candidates) {
            String n = UrlNormalizer.normalize(it.url());
            if (n.isBlank()) continue;
            if (seenUrls.contains(n) || !batchUrls.add(n)) continue;
            if (groundingCheck && !searchUrls.isEmpty() && !searchUrls.contains(n)) {
                log.warn("Skipping (not in search results): {}", it.url());
                continue;
            }
            fresh.add(it);
            if (fresh.size() >= maxItems) break;
        }
        log.info("Kept {} of {} candidate(s) after filtering", fresh.size(), candidates.size());
        return fresh;
    }
}
