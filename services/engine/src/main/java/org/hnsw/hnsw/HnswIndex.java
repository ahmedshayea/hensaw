package org.hnsw.hnsw;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.hnsw.VectorRecord;
import org.hnsw.distance.DistanceFunction;

public final class HnswIndex {
    private final int dimension;
    private final int m;
    private final int efConstruction;
    private final int efSearch;
    private final DistanceFunction distanceFunction;

    private final ConcurrentMap<String, HnswNode> nodes = new ConcurrentHashMap<>();
    private final Random levelRandom = new SecureRandom();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile HnswNode entryPoint;
    private volatile int maxLevel = 0;

    public HnswIndex(int dimension, int m, int efConstruction, int efSearch, DistanceFunction distanceFunction) {
        this.dimension = dimension;
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
        this.distanceFunction = distanceFunction;
    }

    public int getDimension() {
        return dimension;
    }

    public void upsert(VectorRecord record) {
        if (record.values().length != dimension) {
            throw new IllegalArgumentException("Vector dimension mismatch: expected " + dimension + " got " + record.values().length);
        }

        lock.writeLock().lock();
        try {
            HnswNode existing = nodes.get(record.id());
            if (existing != null) {
                remove(existing);
            }
            insert(record);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        return nodes.size();
    }

    public List<SearchResult> search(float[] query, int topK) {
        if (query.length != dimension) {
            throw new IllegalArgumentException("Query dimension mismatch: expected " + dimension + " got " + query.length);
        }

        lock.readLock().lock();
        try {
            if (entryPoint == null) {
                return List.of();
            }

            HnswNode curr = entryPoint;
            double currDist = distanceFunction.distance(query, curr.record.values());
            for (int level = maxLevel; level > 0; level--) {
                boolean changed;
                do {
                    changed = false;
                    for (HnswNode neighbor : curr.neighborsAt(level)) {
                        double dist = distanceFunction.distance(query, neighbor.record.values());
                        if (dist < currDist) {
                            currDist = dist;
                            curr = neighbor;
                            changed = true;
                        }
                    }
                } while (changed);
            }

            PriorityQueue<SearchEntry> candidates = new PriorityQueue<>(Comparator.comparingDouble(a -> a.distance));
            PriorityQueue<SearchEntry> best = new PriorityQueue<>((a, b) -> Double.compare(b.distance, a.distance));
            Set<String> visited = new HashSet<>();

            SearchEntry entry = new SearchEntry(curr, currDist);
            candidates.add(entry);
            best.add(entry);
            visited.add(curr.record.id());

            while (!candidates.isEmpty()) {
                SearchEntry candidate = candidates.poll();
                SearchEntry worstBest = best.peek();
                if (worstBest != null && candidate.distance > worstBest.distance) {
                    break;
                }
                for (HnswNode neighbor : candidate.node.neighborsAt(0)) {
                    if (!visited.add(neighbor.record.id())) {
                        continue;
                    }
                    double dist = distanceFunction.distance(query, neighbor.record.values());
                    SearchEntry neighborEntry = new SearchEntry(neighbor, dist);
                    if (best.size() < efSearch || dist < best.peek().distance) {
                        candidates.add(neighborEntry);
                        best.add(neighborEntry);
                        if (best.size() > efSearch) {
                            best.poll();
                        }
                    }
                }
            }

            List<SearchEntry> ordered = new ArrayList<>(best);
            ordered.sort(Comparator.comparingDouble(a -> a.distance));
            return ordered.stream()
                    .limit(topK)
                    .map(resultEntry -> new SearchResult(resultEntry.node.record, resultEntry.distance))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void insert(VectorRecord record) {
        int level = sampleLevel();
        HnswNode node = new HnswNode(record, level);
        nodes.put(record.id(), node);

        if (entryPoint == null) {
            entryPoint = node;
            maxLevel = level;
            return;
        }

        HnswNode curr = entryPoint;
        double currDist = distanceFunction.distance(record.values(), curr.record.values());
        for (int l = maxLevel; l > level; l--) {
            boolean changed;
            do {
                changed = false;
                for (HnswNode neighbor : curr.neighborsAt(l)) {
                    double dist = distanceFunction.distance(record.values(), neighbor.record.values());
                    if (dist < currDist) {
                        currDist = dist;
                        curr = neighbor;
                        changed = true;
                    }
                }
            } while (changed);
        }

        for (int l = Math.min(level, maxLevel); l >= 0; l--) {
            var neighbors = searchLayer(record.values(), curr, l, efConstruction);
            connect(node, neighbors, l);
        }

        if (level > maxLevel) {
            maxLevel = level;
            entryPoint = node;
        }
    }

    private PriorityQueue<SearchEntry> searchLayer(float[] target, HnswNode entry, int level, int ef) {
        PriorityQueue<SearchEntry> candidates = new PriorityQueue<>((a, b) -> Double.compare(b.distance, a.distance));
        PriorityQueue<SearchEntry> best = new PriorityQueue<>(Comparator.comparingDouble(a -> a.distance));
        Set<String> visited = new HashSet<>();

        double entryDist = distanceFunction.distance(target, entry.record.values());
        SearchEntry first = new SearchEntry(entry, entryDist);
        candidates.add(first);
        best.add(first);
        visited.add(entry.record.id());

        while (!candidates.isEmpty()) {
            SearchEntry farthest = candidates.poll();
            SearchEntry worst = best.peek();
            if (worst != null && farthest.distance > worst.distance) {
                break;
            }
            for (HnswNode neighbor : farthest.node.neighborsAt(level)) {
                if (!visited.add(neighbor.record.id())) {
                    continue;
                }
                double dist = distanceFunction.distance(target, neighbor.record.values());
                SearchEntry candidate = new SearchEntry(neighbor, dist);
                if (best.size() < ef || dist < best.peek().distance) {
                    candidates.add(candidate);
                    best.add(candidate);
                    if (best.size() > ef) {
                        best.poll();
                    }
                }
            }
        }

        return best;
    }

    private void connect(HnswNode node, PriorityQueue<SearchEntry> neighbors, int level) {
        List<SearchEntry> sorted = new ArrayList<>(neighbors);
        sorted.sort(Comparator.comparingDouble(a -> a.distance));
        int added = 0;
        for (SearchEntry neighborEntry : sorted) {
            HnswNode neighbor = neighborEntry.node;
            node.ensureLevel(level);
            neighbor.ensureLevel(level);
            node.neighborsAt(level).add(neighbor);
            neighbor.neighborsAt(level).add(node);
            trim(neighbor.neighborsAt(level));
            if (++added >= m) {
                break;
            }
        }
        trim(node.neighborsAt(level));
    }

    private void trim(Set<HnswNode> neighbors) {
        if (neighbors.size() <= m) {
            return;
        }
        List<HnswNode> ordered = new ArrayList<>(neighbors);
        ordered.sort(Comparator.comparingInt(a -> a.neighborsAt(0).size()));
        while (ordered.size() > m) {
            HnswNode removed = ordered.remove(ordered.size() - 1);
            neighbors.remove(removed);
        }
    }

    private void remove(HnswNode node) {
        for (int level = 0; level <= node.level; level++) {
            for (HnswNode neighbor : new HashSet<>(node.neighborsAt(level))) {
                neighbor.neighborsAt(level).remove(node);
            }
        }
        nodes.remove(node.record.id());
        if (entryPoint == node) {
            entryPoint = nodes.values().stream().findAny().orElse(null);
            maxLevel = entryPoint != null ? entryPoint.level : 0;
        }
    }

    private int sampleLevel() {
        return (int) (-Math.log(levelRandom.nextDouble()) * 0.5);
    }

    public record SearchResult(VectorRecord record, double distance) {}

    private record SearchEntry(HnswNode node, double distance) {}
}
