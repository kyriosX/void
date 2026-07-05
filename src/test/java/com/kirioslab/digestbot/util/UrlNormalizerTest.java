package com.kirioslab.digestbot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UrlNormalizerTest {

    @Test
    void stripsSchemeAndWww() {
        assertEquals("example.com/foo", UrlNormalizer.normalize("https://www.example.com/foo"));
        assertEquals("example.com/foo", UrlNormalizer.normalize("http://example.com/foo"));
    }

    @Test
    void stripsTrailingSlashButNotRoot() {
        assertEquals("example.com/foo", UrlNormalizer.normalize("https://example.com/foo/"));
        assertEquals("example.com", UrlNormalizer.normalize("https://example.com"));
    }

    @Test
    void lowercasesHostButPreservesPathCase() {
        assertEquals("example.com/FooBar", UrlNormalizer.normalize("https://EXAMPLE.com/FooBar"));
    }

    @Test
    void stripsTrackingParamsIndividuallyAndCombined() {
        assertEquals("example.com/foo", UrlNormalizer.normalize("https://example.com/foo?utm_source=x"));
        assertEquals("example.com/foo", UrlNormalizer.normalize("https://example.com/foo?ref=abc"));
        assertEquals("example.com/foo", UrlNormalizer.normalize("https://example.com/foo?fbclid=1&gclid=2&mc_cid=3&mc_eid=4&utm_campaign=y"));
    }

    @Test
    void keepsNonTrackingQueryParams() {
        assertEquals("example.com/foo?id=42", UrlNormalizer.normalize("https://example.com/foo?id=42"));
    }

    @Test
    void cosmeticVariationsNormalizeToSameValue() {
        String a = UrlNormalizer.normalize("https://www.example.com/foo/?utm_source=x");
        String b = UrlNormalizer.normalize("http://example.com/foo");
        assertEquals(a, b);
    }

    @Test
    void malformedOrEmptyInputDoesNotThrow() {
        assertDoesNotThrow(() -> UrlNormalizer.normalize(""));
        assertDoesNotThrow(() -> UrlNormalizer.normalize("not a url ::: at all"));
    }

    @Test
    void nullInputReturnsEmptyString() {
        assertEquals("", UrlNormalizer.normalize(null));
    }
}
