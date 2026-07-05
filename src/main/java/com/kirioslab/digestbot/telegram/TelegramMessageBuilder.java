package com.kirioslab.digestbot.telegram;

import com.kirioslab.digestbot.model.NewsItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Formats a batch of news items into Telegram-HTML messages, chunked under the char limit. */
public final class TelegramMessageBuilder {

    private TelegramMessageBuilder() {}

    public static List<String> buildMessages(List<NewsItem> items, int charLimit) {
        Map<String, List<NewsItem>> byTopic = new LinkedHashMap<>();
        for (NewsItem it : items) byTopic.computeIfAbsent(it.topic(), k -> new ArrayList<>()).add(it);

        List<String> messages = new ArrayList<>();
        StringBuilder cur = new StringBuilder("<b>Dev digest — " + LocalDate.now() + "</b>\n");
        for (var entry : byTopic.entrySet()) {
            StringBuilder block = new StringBuilder("\n<b>" + escapeHtml(entry.getKey()) + "</b>\n");
            for (NewsItem it : entry.getValue()) {
                block.append("• <a href=\"").append(escapeHtml(it.url())).append("\">")
                     .append(escapeHtml(it.title())).append("</a>");
                if (!it.why().isBlank()) block.append(" — ").append(escapeHtml(it.why()));
                block.append("\n");
            }
            if (cur.length() + block.length() > charLimit) { messages.add(cur.toString()); cur = new StringBuilder(); }
            cur.append(block);
        }
        if (cur.length() > 0) messages.add(cur.toString());
        return messages;
    }

    public static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
