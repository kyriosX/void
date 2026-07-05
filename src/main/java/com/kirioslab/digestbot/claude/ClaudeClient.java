package com.kirioslab.digestbot.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.WebSearchTool20260318;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Only class that talks to the Anthropic Messages API. */
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final AnthropicClient client;
    private final String model;

    public ClaudeClient(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    public Message ask(String recentList) {
        log.info("Asking Claude ({}) for today's digest, with web search enabled...", model);
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2048L)
                .addTool(WebSearchTool20260318.builder()
                        .maxUses(8L)
                        .responseInclusion(WebSearchTool20260318.ResponseInclusion.EXCLUDED)
                        .build())
                .system(ClaudeResponseParser.systemPrompt(recentList))
                .addUserMessage("Run today's digest.")
                .build();
        Message response = client.messages().create(params);
        log.info("Claude responded with {} content block(s), stop reason: {}",
                response.content().size(), response.stopReason().map(Object::toString).orElse("n/a"));
        return response;
    }
}
