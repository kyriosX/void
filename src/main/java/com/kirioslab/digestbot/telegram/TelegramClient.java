package com.kirioslab.digestbot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/** Only class that talks to the Telegram Bot API. */
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final OkHttpTelegramClient client;
    private final String channelId;

    public TelegramClient(String botToken, String channelId) {
        this.client = new OkHttpTelegramClient(botToken);
        this.channelId = channelId;
    }

    public void send(String text) throws TelegramApiException {
        log.info("Sending Telegram message ({} chars) to {}...", text.length(), channelId);
        SendMessage message = SendMessage.builder()
                .chatId(channelId) // numeric id (-100…) for private channels, or "@name" for public ones
                .text(text)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(true)
                .build();
        client.execute(message);
        log.info("Telegram message sent.");
    }
}
