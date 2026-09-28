package com.vectordb.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorMathTest {

    // small tolerance for floating-point comparisons
    private static final double DELTA = 1e-9;

    // ── Cosine Similarity ─────────────────────────────────────────────────

    @Nested
    @DisplayName("cosineSimilarity")
    class CosineSimilarityTests {

        @Test
        @DisplayName("identical vectors → 1.0")
        void identicalVectors() {
            double[] v = {1.0, 2.0, 3.0};
            assertEquals(1.0, VectorMath.cosineSimilarity(v, v), DELTA);
        }

        @Test
        @DisplayName("opposite vectors → -1.0")
        void oppositeVectors() {
            double[] a = { 1.0,  2.0,  3.0};
            double[] b = {-1.0, -2.0, -3.0};
            assertEquals(-1.0, VectorMath.cosineSimilarity(a, b), DELTA);
        }

        @Test
        @DisplayName("orthogonal vectors → 0.0")
        void orthogonalVectors() {
            double[] a = {1.0, 0.0};
            double[] b = {0.0, 1.0};
            assertEquals(0.0, VectorMath.cosineSimilarity(a, b), DELTA);
        }

        @Test
        @DisplayName("zero vector → 0.0 (no division by zero)")
        void zeroVector() {
            double[] a = {0.0, 0.0, 0.0};
            double[] b = {1.0, 2.0, 3.0};
            assertEquals(0.0, VectorMath.cosineSimilarity(a, b), DELTA);
        }

        @Test
        @DisplayName("both zero vectors → 0.0")
        void bothZeroVectors() {
            double[] a = {0.0, 0.0};
            double[] b = {0.0, 0.0};
            assertEquals(0.0, VectorMath.cosineSimilarity(a, b), DELTA);
        }

        @Test
        @DisplayName("unit vectors in same direction → 1.0")
        void unitVectorsSameDirection() {
            double[] a = {1.0, 0.0, 0.0};
            double[] b = {1.0, 0.0, 0.0};
            assertEquals(1.0, VectorMath.cosineSimilarity(a, b), DELTA);
        }
    }

    // ── Cosine Distance ───────────────────────────────────────────────────

    @Nested
    @DisplayName("cosineDistance")
    class CosineDistanceTests {

        @Test
        @DisplayName("identical vectors → 0.0")
        void identicalVectors() {
            double[] v = {1.0, 2.0, 3.0};
            assertEquals(0.0, VectorMath.cosineDistance(v, v), DELTA);
        }

        @Test
        @DisplayName("opposite vectors → 2.0")
        void oppositeVectors() {
            double[] a = { 1.0,  2.0};
            double[] b = {-1.0, -2.0};
            assertEquals(2.0, VectorMath.cosineDistance(a, b), DELTA);
        }

        @Test
        @DisplayName("orthogonal vectors → 1.0")
        void orthogonalVectors() {
            double[] a = {1.0, 0.0};
            double[] b = {0.0, 1.0};
            assertEquals(1.0, VectorMath.cosineDistance(a, b), DELTA);
        }

        @Test
        @DisplayName("distance is 1 - similarity")
        void distanceIsOneMinusSimilarity() {
            double[] a = {1.0, 2.0, 3.0};
            double[] b = {4.0, 5.0, 6.0};
            double similarity = VectorMath.cosineSimilarity(a, b);
            double distance   = VectorMath.cosineDistance(a, b);
            assertEquals(1.0 - similarity, distance, DELTA);
        }
    }

    // ── Euclidean Distance ────────────────────────────────────────────────

    @Nested
    @DisplayName("euclideanDistance")
    class EuclideanDistanceTests {

        @Test
        @DisplayName("identical vectors → 0.0")
        void identicalVectors() {
            double[] v = {3.0, 4.0};
            assertEquals(0.0, VectorMath.euclideanDistance(v, v), DELTA);
        }

        @Test
        @DisplayName("3-4-5 right triangle → 5.0")
        void knownDistance() {
            double[] a = {0.0, 0.0};
            double[] b = {3.0, 4.0};
            assertEquals(5.0, VectorMath.euclideanDistance(a, b), DELTA);
        }

        @Test
        @DisplayName("single dimension → absolute difference")
        void singleDimension() {
            double[] a = {1.0};
            double[] b = {4.0};
            assertEquals(3.0, VectorMath.euclideanDistance(a, b), DELTA);
        }

        @Test
        @DisplayName("symmetric: dist(a,b) == dist(b,a)")
        void symmetry() {
            double[] a = {1.0, 5.0, 3.0};
            double[] b = {4.0, 2.0, 6.0};
            assertEquals(
                VectorMath.euclideanDistance(a, b),
                VectorMath.euclideanDistance(b, a),
                DELTA
            );
        }

        @Test
        @DisplayName("negative values handled correctly")
        void negativeValues() {
            double[] a = {-1.0, -1.0};
            double[] b = { 1.0,  1.0};
            assertEquals(Math.sqrt(8.0), VectorMath.euclideanDistance(a, b), DELTA);
        }
    }

    // ── Manhattan Distance ────────────────────────────────────────────────

    @Nested
    @DisplayName("manhattanDistance")
    class ManhattanDistanceTests {

        @Test
        @DisplayName("identical vectors → 0.0")
        void identicalVectors() {
            double[] v = {1.0, 2.0, 3.0};
            assertEquals(0.0, VectorMath.manhattanDistance(v, v), DELTA);
        }

        @Test
        @DisplayName("known values: |1-4| + |2-6| = 7.0")
        void knownDistance() {
            double[] a = {1.0, 2.0};
            double[] b = {4.0, 6.0};
            assertEquals(7.0, VectorMath.manhattanDistance(a, b), DELTA);
        }

        @Test
        @DisplayName("symmetric: dist(a,b) == dist(b,a)")
        void symmetry() {
            double[] a = {1.0, 5.0};
            double[] b = {3.0, 2.0};
            assertEquals(
                VectorMath.manhattanDistance(a, b),
                VectorMath.manhattanDistance(b, a),
                DELTA
            );
        }

        @Test
        @DisplayName("negative values: absolute difference used")
        void negativeValues() {
            double[] a = {-3.0};
            double[] b = { 3.0};
            assertEquals(6.0, VectorMath.manhattanDistance(a, b), DELTA);
        }
    }

    // ── Distance Dispatch ─────────────────────────────────────────────────

    @Nested
    @DisplayName("distance (dispatch)")
    class DistanceDispatchTests {

        @Test
        @DisplayName("'cosine' routes to cosineDistance")
        void cosineRouting() {
            double[] a = {1.0, 0.0};
            double[] b = {0.0, 1.0};
            assertEquals(
                VectorMath.cosineDistance(a, b),
                VectorMath.distance(a, b, "cosine"),
                DELTA
            );
        }

        @Test
        @DisplayName("'euclidean' routes to euclideanDistance")
        void euclideanRouting() {
            double[] a = {0.0, 0.0};
            double[] b = {3.0, 4.0};
            assertEquals(
                VectorMath.euclideanDistance(a, b),
                VectorMath.distance(a, b, "euclidean"),
                DELTA
            );
        }

        @Test
        @DisplayName("'manhattan' routes to manhattanDistance")
        void manhattanRouting() {
            double[] a = {1.0, 2.0};
            double[] b = {4.0, 6.0};
            assertEquals(
                VectorMath.manhattanDistance(a, b),
                VectorMath.distance(a, b, "manhattan"),
                DELTA
            );
        }

        @Test
        @DisplayName("unknown metric defaults to cosine")
        void unknownMetricDefaultsToCosine() {
            double[] a = {1.0, 2.0};
            double[] b = {3.0, 4.0};
            assertEquals(
                VectorMath.cosineDistance(a, b),
                VectorMath.distance(a, b, "unknown_metric"),
                DELTA
            );
        }

        @Test
        @DisplayName("metric matching is case-insensitive")
        void caseInsensitive() {
            double[] a = {1.0, 0.0};
            double[] b = {0.0, 1.0};
            assertEquals(
                VectorMath.distance(a, b, "cosine"),
                VectorMath.distance(a, b, "COSINE"),
                DELTA
            );
        }
    }

    // ── Conversion Helpers ────────────────────────────────────────────────

    @Nested
    @DisplayName("toArray / toList")
    class ConversionTests {

        @Test
        @DisplayName("toArray converts List<Double> correctly")
        void toArray() {
            List<Double> list = List.of(1.0, 2.0, 3.0);
            double[] result = VectorMath.toArray(list);
            assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result, DELTA);
        }

        @Test
        @DisplayName("toArray with null → empty array")
        void toArrayNull() {
            assertArrayEquals(new double[0], VectorMath.toArray(null));
        }

        @Test
        @DisplayName("toArray with empty list → empty array")
        void toArrayEmpty() {
            assertArrayEquals(new double[0], VectorMath.toArray(List.of()));
        }

        @Test
        @DisplayName("toList converts double[] correctly")
        void toList() {
            double[] array = {1.0, 2.0, 3.0};
            List<Double> result = VectorMath.toList(array);
            assertEquals(List.of(1.0, 2.0, 3.0), result);
        }

        @Test
        @DisplayName("toList with null → empty list")
        void toListNull() {
            assertEquals(List.of(), VectorMath.toList(null));
        }

        @Test
        @DisplayName("toArray then toList round-trips correctly")
        void roundTrip() {
            List<Double> original = List.of(0.5, 1.5, 2.5);
            List<Double> result = VectorMath.toList(VectorMath.toArray(original));
            assertEquals(original, result);
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validation")
    class ValidationTests {

        @Test
        @DisplayName("mismatched lengths → IllegalArgumentException")
        void mismatchedLengths() {
            double[] a = {1.0, 2.0};
            double[] b = {1.0, 2.0, 3.0};
            assertThrows(IllegalArgumentException.class,
                () -> VectorMath.cosineDistance(a, b));
        }

        @Test
        @DisplayName("null vector → IllegalArgumentException")
        void nullVector() {
            double[] a = {1.0, 2.0};
            assertThrows(IllegalArgumentException.class,
                () -> VectorMath.cosineDistance(null, a));
        }

        @Test
        @DisplayName("validateDimensions: correct size passes silently")
        void validateDimensionsPass() {
            double[] v = {1.0, 2.0, 3.0};
            assertDoesNotThrow(() -> VectorMath.validateDimensions(v, 3));
        }

        @Test
        @DisplayName("validateDimensions: wrong size → IllegalArgumentException")
        void validateDimensionsFail() {
            double[] v = {1.0, 2.0};
            assertThrows(IllegalArgumentException.class,
                () -> VectorMath.validateDimensions(v, 16));
        }

        @Test
        @DisplayName("validateDimensions: null vector → IllegalArgumentException")
        void validateDimensionsNull() {
            assertThrows(IllegalArgumentException.class,
                () -> VectorMath.validateDimensions(null, 16));
        }
    }
}