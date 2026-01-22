package org.hnsw;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record VectorRecord(String id, float[] values, Map<String, String> metadata) {

    public VectorRecord {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Vector id must be provided");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Vector values must not be empty");
        }
        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public int dimension() {
        return values.length;
    }
}
