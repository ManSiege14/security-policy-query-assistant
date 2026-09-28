package com.vectordb.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build a string of N words: "word1 word2 word3 ..." */
    private static String words(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) sb.append(' ');
            sb.append("word").append(i);
        }
        return sb.toString();
    }

    private static int countWords(String text) {
        return text.strip().split("\\s+").length;
    }

    // -------------------------------------------------------------------------
    // Null / blank input
    // -------------------------------------------------------------------------
   
    @Test
    void nullTextReturnsEmpty() {
        List<String> result = TextChunker.chunk(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void blankTextReturnsEmpty() {
        assertTrue(TextChunker.chunk("").isEmpty());
        assertTrue(TextChunker.chunk("   ").isEmpty());
        assertTrue(TextChunker.chunk("\n\t").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Single-chunk cases (text shorter than chunkWords)
    // -------------------------------------------------------------------------

    @Test
    void shortTextReturnsSingleChunk() {
        String text = "hello world this is short";
        List<String> chunks = TextChunker.chunk(text, 250, 30);

        assertEquals(1, chunks.size());
        assertEquals("hello world this is short", chunks.get(0));
    }

    @Test
    void textExactlyChunkSizeReturnsSingleChunk() {
        String text = words(10);
        List<String> chunks = TextChunker.chunk(text, 10, 2);

        assertEquals(1, chunks.size());
        assertEquals(10, countWords(chunks.get(0)));
    }

    @Test
    void singleWordTextReturnsSingleChunk() {
        List<String> chunks = TextChunker.chunk("hello", 250, 30);
        assertEquals(1, chunks.size());
        assertEquals("hello", chunks.get(0));
    }

    // -------------------------------------------------------------------------
    // Multi-chunk cases
    // -------------------------------------------------------------------------

    @Test
    void textLongerThanChunkSizeProducesMultipleChunks() {
        String text = words(300);
        List<String> chunks = TextChunker.chunk(text, 250, 30);

        assertTrue(chunks.size() > 1,
                "Expected multiple chunks for 300-word text with chunkWords=250");
    }

    @Test
    void eachChunkHasAtMostChunkWordsWords() {
        String text = words(600);
        List<String> chunks = TextChunker.chunk(text, 100, 20);

        for (String chunk : chunks) {
            int wc = countWords(chunk);
            assertTrue(wc <= 100,
                    "Chunk exceeded chunkWords limit: " + wc + " words");
        }
    }

    @Test
    void lastChunkMayBeShorterThanChunkWords() {
        // 110 words, chunkWords=100, overlap=10 → step=90
        // chunk1: words 0-99 (100 words)
        // chunk2: words 90-109 (20 words — remainder)
        String text = words(110);
        List<String> chunks = TextChunker.chunk(text, 100, 10);

        assertEquals(2, chunks.size());
        assertEquals(100, countWords(chunks.get(0)));
        assertEquals(20,  countWords(chunks.get(1)));
    }

    // -------------------------------------------------------------------------
    // Overlap correctness
    // -------------------------------------------------------------------------

    @Test
    void consecutiveChunksShareOverlapWords() {
        int chunkWords   = 10;
        int overlapWords = 3;

        // 20 words: chunk1 = words 0-9, chunk2 = words 7-16, chunk3 = words 14-19
        String text = words(20);
        List<String> chunks = TextChunker.chunk(text, chunkWords, overlapWords);

        assertTrue(chunks.size() >= 2);

        String[] first  = chunks.get(0).split(" ");
        String[] second = chunks.get(1).split(" ");

        // Last `overlapWords` of first chunk == first `overlapWords` of second chunk
        for (int i = 0; i < overlapWords; i++) {
            assertEquals(
                    first[first.length - overlapWords + i],
                    second[i],
                    "Overlap mismatch at position " + i
            );
        }
    }

    @Test
    void zeroOverlapProducesNonOverlappingChunks() {
        int chunkWords = 5;
        String text    = words(15);
        List<String> chunks = TextChunker.chunk(text, chunkWords, 0);

        // 15 words / 5 per chunk = 3 chunks, no overlap
        assertEquals(3, chunks.size());

        String[] first  = chunks.get(0).split(" ");
        String[] second = chunks.get(1).split(" ");

        // Last word of first chunk should NOT equal first word of second
        assertNotEquals(first[first.length - 1], second[0]);
    }

    // -------------------------------------------------------------------------
    // Whitespace handling
    // -------------------------------------------------------------------------

    @Test
    void extraWhitespaceIsTreatedAsOneDelimiter() {
        String text = "one   two\tthree\nfour";
        List<String> chunks = TextChunker.chunk(text, 250, 30);

        assertEquals(1, chunks.size());
        assertEquals("one two three four", chunks.get(0));
    }

    @Test
    void leadingAndTrailingWhitespaceIsIgnored() {
        String text = "  hello world  ";
        List<String> chunks = TextChunker.chunk(text, 250, 30);

        assertEquals(1, chunks.size());
        assertEquals("hello world", chunks.get(0));
    }

    // -------------------------------------------------------------------------
    // wordCount utility
    // -------------------------------------------------------------------------

    @Test
    void wordCountReturnsCorrectCount() {
        assertEquals(5,  TextChunker.wordCount("one two three four five"));
        assertEquals(1,  TextChunker.wordCount("single"));
        assertEquals(0,  TextChunker.wordCount(""));
        assertEquals(0,  TextChunker.wordCount(null));
        assertEquals(3,  TextChunker.wordCount("  a   b   c  "));
    }

    // -------------------------------------------------------------------------
    // Default constants
    // -------------------------------------------------------------------------

    @Test
    void defaultConstantsHaveExpectedValues() {
        assertEquals(250, TextChunker.DEFAULT_CHUNK_WORDS);
        assertEquals(30,  TextChunker.DEFAULT_OVERLAP_WORDS);
    }

    @Test
    void defaultChunkMatchesExplicitCall() {
        String text = words(400);
        List<String> defaultResult   = TextChunker.chunk(text);
        List<String> explicitResult  = TextChunker.chunk(text, 250, 30);

        assertEquals(defaultResult.size(), explicitResult.size());
        for (int i = 0; i < defaultResult.size(); i++) {
            assertEquals(defaultResult.get(i), explicitResult.get(i));
        }
    }

    // -------------------------------------------------------------------------
    // Invalid parameter guards
    // -------------------------------------------------------------------------

    @Test
    void zeroChunkWordsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextChunker.chunk("hello", 0, 0));
    }

    @Test
    void negativeChunkWordsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextChunker.chunk("hello", -5, 0));
    }

    @Test
    void negativeOverlapThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextChunker.chunk("hello", 10, -1));
    }

    @Test
    void overlapEqualToChunkWordsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextChunker.chunk("hello", 10, 10));
    }

    @Test
    void overlapGreaterThanChunkWordsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextChunker.chunk("hello", 10, 15));
    }

    // -------------------------------------------------------------------------
    // Coverage for exact chunk boundaries
    // -------------------------------------------------------------------------

    @Test
    void textOneWordOverChunkSizeProducesTwoChunks() {
        // chunkWords=5, overlap=1 → step=4
        // 6 words: chunk1 = words 0-4, chunk2 = words 4-5
        String text = words(6);
        List<String> chunks = TextChunker.chunk(text, 5, 1);

        assertEquals(2, chunks.size());
        assertEquals(5, countWords(chunks.get(0)));
        assertEquals(2, countWords(chunks.get(1)));
    }

    @Test
    void largeTextWith500WordsHasCorrectChunkCount() {
        // chunkWords=250, overlap=30, step=220
        // chunks start at: 0, 220, 440 → 3 chunks
        String text = words(500);
        List<String> chunks = TextChunker.chunk(text, 250, 30);

        assertEquals(3, chunks.size());
        assertEquals(250, countWords(chunks.get(0)));
        assertEquals(250, countWords(chunks.get(1)));
        assertEquals(60,  countWords(chunks.get(2)));  // 500 - 440 = 60
    }
}