package org.hnsw.hnsw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hnsw.VectorRecord;

final class HnswNode {
    final VectorRecord record;
    final int level;
    private final List<Set<HnswNode>> neighbors;

    HnswNode(VectorRecord record, int level) {
        this.record = record;
        this.level = level;
        this.neighbors = new ArrayList<>();
        for (int i = 0; i <= level; i++) {
            neighbors.add(new HashSet<>());
        }
    }

    Set<HnswNode> neighborsAt(int layer) {
        if (layer >= neighbors.size()) {
            return neighbors.get(neighbors.size() - 1);
        }
        return neighbors.get(layer);
    }

    void ensureLevel(int targetLevel) {
        while (neighbors.size() <= targetLevel) {
            neighbors.add(new HashSet<>());
        }
    }
}
