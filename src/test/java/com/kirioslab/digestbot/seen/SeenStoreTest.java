package com.kirioslab.digestbot.seen;

import com.kirioslab.digestbot.model.SeenItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenStoreTest {

    @Test
    void loadOnMissingPathReturnsEmptyList(@TempDir Path dir) throws Exception {
        SeenStore store = new SeenStore(dir.resolve("nonexistent.json"));
        assertEquals(List.of(), store.load());
    }

    @Test
    void loadOnZeroByteFileReturnsEmptyList(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("empty.json");
        Files.createFile(path);
        SeenStore store = new SeenStore(path);
        assertEquals(List.of(), store.load());
    }

    @Test
    void saveThenLoadRoundTrips(@TempDir Path dir) throws Exception {
        SeenStore store = new SeenStore(dir.resolve("seen.json"));
        List<SeenItem> items = List.of(
                new SeenItem("https://example.com/1", "T1", "2026-01-01"),
                new SeenItem("https://example.com/2", "T2", "2026-01-02"));
        store.save(items);
        assertEquals(items, store.load());
    }

    @Test
    void saveRefusesToShrinkExistingList(@TempDir Path dir) throws Exception {
        SeenStore store = new SeenStore(dir.resolve("seen.json"));
        List<SeenItem> original = List.of(
                new SeenItem("https://example.com/1", "T1", "2026-01-01"),
                new SeenItem("https://example.com/2", "T2", "2026-01-02"));
        store.save(original);

        assertThrows(IllegalStateException.class,
                () -> store.save(List.of(new SeenItem("https://example.com/1", "T1", "2026-01-01"))));
        assertEquals(original, store.load()); // rejected write must not touch the file
    }

    @Test
    void saveAllowsEqualOrGrowingList(@TempDir Path dir) throws Exception {
        SeenStore store = new SeenStore(dir.resolve("seen.json"));
        SeenItem item = new SeenItem("https://example.com/1", "T1", "2026-01-01");
        store.save(List.of(item));
        store.save(List.of(item)); // same size — allowed
        store.save(List.of(item, new SeenItem("https://example.com/2", "T2", "2026-01-02"))); // grows — allowed
        assertEquals(2, store.load().size());
    }

    @Test
    void saveCreatesMissingParentDirectories(@TempDir Path dir) throws Exception {
        Path nested = dir.resolve("state/seen.json");
        SeenStore store = new SeenStore(nested);
        store.save(List.of(new SeenItem("https://example.com", "T", "2026-01-01")));
        assertTrue(Files.exists(nested));
    }

    @Test
    void recentSliceReturnsLastNWhenWindowSmallerThanList() {
        List<SeenItem> seen = List.of(
                new SeenItem("u1", "T1", "d1"),
                new SeenItem("u2", "T2", "d2"),
                new SeenItem("u3", "T3", "d3"));
        String slice = SeenStore.recentSlice(seen, 2);
        assertTrue(slice.contains("T2") && slice.contains("T3"));
        assertTrue(!slice.contains("T1"));
    }

    @Test
    void recentSliceReturnsEverythingWhenWindowLargerThanList() {
        List<SeenItem> seen = List.of(new SeenItem("u1", "T1", "d1"));
        String slice = SeenStore.recentSlice(seen, 100);
        assertTrue(slice.contains("T1"));
    }

    @Test
    void recentSliceOnEmptyListReturnsEmptyString() {
        assertEquals("", SeenStore.recentSlice(List.of(), 10));
    }
}
