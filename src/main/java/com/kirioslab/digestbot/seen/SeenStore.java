package com.kirioslab.digestbot.seen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirioslab.digestbot.model.SeenItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads/writes the git-committed "database": a JSON array of previously-posted items. */
public class SeenStore {

    private static final Logger log = LoggerFactory.getLogger(SeenStore.class);
    private static final ObjectMapper M = new ObjectMapper();
    private static final Path DEFAULT_PATH = Path.of("resources/seen.json");

    private final Path path;

    /** Uses the tracked resource file — the git-committed "database" the workflow commits back. */
    public SeenStore() {
        this(DEFAULT_PATH);
    }

    public SeenStore(Path path) {
        this.path = path;
    }

    public List<SeenItem> load() throws IOException {
        if (!Files.exists(path)) {
            log.info("No seen-list at {} yet — starting from empty.", path);
            return new ArrayList<>();
        }
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length == 0) {
            log.info("Seen-list at {} is empty — starting from empty.", path);
            return new ArrayList<>();
        }
        List<SeenItem> out = new ArrayList<>();
        for (JsonNode n : M.readTree(bytes)) {
            out.add(new SeenItem(n.path("url").asText(""), n.path("title").asText(""), n.path("date").asText("")));
        }
        log.info("Loaded {} previously-seen item(s) from {}", out.size(), path);
        return out;
    }

    public void save(List<SeenItem> seen) throws IOException {
        int existingCount = load().size();
        if (seen.size() < existingCount) {
            throw new IllegalStateException(
                    "Refusing to shrink " + path + ": has " + existingCount + " item(s), would write " + seen.size());
        }
        Files.createDirectories(path.getParent());
        M.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), seen);
        log.info("Saved {} total seen item(s) to {} ({} new)", seen.size(), path, seen.size() - existingCount);
    }

    public static String recentSlice(List<SeenItem> seen, int window) {
        int from = Math.max(0, seen.size() - window);
        StringBuilder sb = new StringBuilder();
        for (SeenItem s : seen.subList(from, seen.size()))
            sb.append("- ").append(s.title()).append(" (").append(s.url()).append(")\n");
        return sb.toString();
    }
}
