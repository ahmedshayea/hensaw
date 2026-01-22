package org.hnsw.distance;

public final class CosineDistance implements DistanceFunction {
    @Override
    public double distance(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 1.0;
        }
        double similarity = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1.0 - similarity;
    }
}
