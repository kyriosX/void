package com.kirioslab.digestbot.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class UrlNormalizer {

    private UrlNormalizer() {}

    /** Collapses http/https, www., trailing slash, and tracking params so the same article matches. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        try {
            URI u = URI.create(raw.trim());
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase().replaceFirst("^www\\.", "");
            if (host.isEmpty()) return raw.trim().toLowerCase();
            String path = u.getPath() == null ? "" : u.getPath();
            if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
            String query = "";
            if (u.getQuery() != null) {
                List<String> kept = new ArrayList<>();
                for (String p : u.getQuery().split("&")) {
                    String key = p.split("=", 2)[0].toLowerCase();
                    if (!(key.startsWith("utm_") || key.equals("ref") || key.equals("fbclid")
                            || key.equals("gclid") || key.equals("mc_cid") || key.equals("mc_eid")))
                        kept.add(p);
                }
                if (!kept.isEmpty()) query = "?" + String.join("&", kept);
            }
            return host + path + query; // scheme dropped on purpose
        } catch (Exception e) {
            return raw.trim().toLowerCase();
        }
    }
}
