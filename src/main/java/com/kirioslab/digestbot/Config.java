package com.kirioslab.digestbot;

/**
 * Immutable run configuration. Secrets come from required env vars (set by the
 * GitHub Actions workflow from repo secrets); tunables are plain defaults.
 */
public record Config(
        String anthropicApiKey,
        String telegramBotToken,
        String telegramChannelId,
        String model,
        int recentWindow,
        int maxItems,
        int tgLimit,
        boolean groundingCheck
) {
    public static Config fromEnv() {
        return new Config(
                requireEnv("ANTHROPIC_API_KEY"),
                requireEnv("TELEGRAM_BOT_TOKEN"),
                requireEnv("TELEGRAM_CHANNEL_ID"),
                "claude-sonnet-4-6",       // cheap enough for a daily run
                60,   // how many recent titles to show Claude so it avoids repeats
                15,   // cap items per digest
                3800, // stay under Telegram's ~4096-char message cap
                true  // drop links that didn't appear in search results
        );
    }

    private static String requireEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing required env var: " + name);
        return v;
    }
}
