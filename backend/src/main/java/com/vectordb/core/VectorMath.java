package com.vectordb.core;

import java.util.List;

/**
 * Pure vector math utilities.
 * No Spring annotations, no dependencies — fully testable in isolation.
 * All methods are static; this class is never instantiated.
 */
public final class VectorMath {

    private VectorMath() {
        // utility class, no instances
    }

    // ── Similarity / Distance ─────────────────────────────────────────────

    /**
     * Cosine similarity between two vectors.
     * Returns a value in [-1.0, 1.0] where 1.0 means identical direction.
     * Returns 0.0 if either vector is a zero vector.
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        validateSameLength(a, b);

        double dot   = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator < 1e-9) return 0.0;

        return dot / denominator;
    }

    /**
     * Cosine distance between two vectors.
     * Returns a value in [0.0, 2.0] where 0.0 means identical direction.
     * Lower is more similar — consistent with euclidean and manhattan.
     */
    public static double cosineDistance(double[] a, double[] b) {
        return 1.0 - cosineSimilarity(a, b);
    }

    /**
     * Euclidean (L2) distance between two vectors.
     * Returns 0.0 for identical vectors.
     */
    public static double euclideanDistance(double[] a, double[] b) {
        validateSameLength(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * Manhattan (L1) distance between two vectors.
     * Sum of absolute differences per dimension.
     * Returns 0.0 for identical vectors.
     */
    public static double manhattanDistance(double[] a, double[] b) {
        validateSameLength(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }

        return sum;
    }

    // ── Dispatch ──────────────────────────────────────────────────────────

    /**
     * Routes to the correct distance function by name.
     * Accepted values: "cosine", "euclidean", "manhattan".
     * Defaults to cosine for any unrecognised value.
     */
    public static double distance(double[] a, double[] b, String metric) {
        return switch (metric.toLowerCase()) {
            case "euclidean" -> euclideanDistance(a, b);
            case "manhattan" -> manhattanDistance(a, b);
            default          -> cosineDistance(a, b);
        };
    }

    // ── Conversion helpers ────────────────────────────────────────────────

    /**
     * Converts a List<Double> to a primitive double[].
     * Used when receiving embeddings from JSON request bodies (Jackson
     * deserialises number arrays as List<Double>).
     */
    public static double[] toArray(List<Double> list) {
        if (list == null || list.isEmpty()) return new double[0];

        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * Converts a primitive double[] to a List<Double>.
     * Used when serialising embeddings into JSON response bodies.
     */
    public static List<Double> toList(double[] array) {
        if (array == null || array.length == 0) return List.of();

        Double[] boxed = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            boxed[i] = array[i];
        }
        return List.of(boxed);
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Validates that two vectors have the same length.
     * Throws IllegalArgumentException with a clear message if not.
     */
    public static void validateSameLength(double[] a, double[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                "Vector length mismatch: " + a.length + " vs " + b.length
            );
        }
    }

    /**
     * Validates that a vector matches an expected dimension count.
     * Used by VectorStoreService when inserting into a fixed-dimension store.
     */
    public static void validateDimensions(double[] vector, int expectedDims) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector must not be null");
        }
        if (vector.length != expectedDims) {
            throw new IllegalArgumentException(
                "Expected " + expectedDims + " dimensions, got " + vector.length
            );
        }
    }

}