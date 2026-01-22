package org.hnsw.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.hnsw.distance.CosineDistance;
import org.hnsw.distance.DistanceFunction;
import org.hnsw.hnsw.HnswIndex;

public final class NamespaceIndexRegistry {
    private final ConcurrentMap<String, HnswIndex> indexes = new ConcurrentHashMap<>();
    private final int m;
    private final int efConstruction;
    private final int efSearch;
    private final DistanceFunction distanceFunction;

    public NamespaceIndexRegistry(int m, int efConstruction, int efSearch) {
        this(m, efConstruction, efSearch, new CosineDistance());
    }

    public NamespaceIndexRegistry(int m, int efConstruction, int efSearch, DistanceFunction distanceFunction) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
        this.distanceFunction = distanceFunction;
    }

    public HnswIndex getOrCreate(String namespace, int dimension) {
        return indexes.compute(namespace, (ns, index) -> {
            if (index == null) {
                return new HnswIndex(dimension, m, efConstruction, efSearch, distanceFunction);
            }
            if (index.getDimension() != dimension) {
                throw new IllegalArgumentException("Namespace '" + namespace + "' expects vectors with dimension " + index.getDimension());
            }
            return index;
        });
    }

    public HnswIndex get(String namespace) {
        return indexes.get(namespace);
    }

    public Map<String, Integer> describe() {
        return indexes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }
}
