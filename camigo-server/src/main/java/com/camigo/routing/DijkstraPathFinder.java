package com.camigo.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** 对正权校园道路图执行 Dijkstra 最短路径计算。 */
public final class DijkstraPathFinder implements ShortestPathProvider {

    private final Set<Long> locations;
    private final Map<Long, List<Neighbor>> adjacency;

    public DijkstraPathFinder(Iterable<Long> locationIds, Iterable<GraphEdge> edges) {
        Set<Long> nodeSet = new HashSet<>();
        locationIds.forEach(nodeSet::add);
        if (nodeSet.isEmpty() || nodeSet.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("校园地点集合不能为空且编号必须有效");
        }

        Map<Long, List<Neighbor>> mutableAdjacency = new HashMap<>();
        nodeSet.forEach(id -> mutableAdjacency.put(id, new ArrayList<>()));
        for (GraphEdge edge : edges) {
            if (!nodeSet.contains(edge.fromLocationId()) || !nodeSet.contains(edge.toLocationId())) {
                throw new IllegalArgumentException("道路引用了未启用的校园地点");
            }
            mutableAdjacency.get(edge.fromLocationId())
                    .add(new Neighbor(edge.toLocationId(), edge.distanceMeters()));
            if (edge.bidirectional()) {
                mutableAdjacency.get(edge.toLocationId())
                        .add(new Neighbor(edge.fromLocationId(), edge.distanceMeters()));
            }
        }
        mutableAdjacency.values().forEach(neighbors ->
                neighbors.sort(Comparator.comparingLong(Neighbor::locationId)
                        .thenComparingInt(Neighbor::distanceMeters)));
        this.locations = Set.copyOf(nodeSet);
        this.adjacency = Map.copyOf(mutableAdjacency);
    }

    @Override
    public ShortestPath shortestPath(long fromLocationId, long toLocationId) {
        requireLocation(fromLocationId);
        requireLocation(toLocationId);
        if (fromLocationId == toLocationId) {
            return new ShortestPath(0, List.of(fromLocationId));
        }

        Map<Long, Long> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparingLong(NodeDistance::distance)
                        .thenComparingLong(NodeDistance::locationId));
        distances.put(fromLocationId, 0L);
        queue.add(new NodeDistance(fromLocationId, 0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.distance() != distances.getOrDefault(current.locationId(), Long.MAX_VALUE)) {
                continue;
            }
            if (current.locationId() == toLocationId) {
                break;
            }
            for (Neighbor neighbor : adjacency.get(current.locationId())) {
                long candidate = Math.addExact(current.distance(), neighbor.distanceMeters());
                long known = distances.getOrDefault(neighbor.locationId(), Long.MAX_VALUE);
                if (candidate < known) {
                    distances.put(neighbor.locationId(), candidate);
                    previous.put(neighbor.locationId(), current.locationId());
                    queue.add(new NodeDistance(neighbor.locationId(), candidate));
                }
            }
        }

        Long distance = distances.get(toLocationId);
        if (distance == null) {
            throw new IllegalStateException("校园地点之间不可达: "
                    + fromLocationId + " -> " + toLocationId);
        }
        if (distance > Integer.MAX_VALUE) {
            throw new IllegalStateException("最短路径距离超出可保存范围");
        }
        List<Long> path = new ArrayList<>();
        Long cursor = toLocationId;
        while (cursor != null) {
            path.add(cursor);
            cursor = previous.get(cursor);
        }
        Collections.reverse(path);
        return new ShortestPath(distance.intValue(), path);
    }

    private void requireLocation(long locationId) {
        if (!locations.contains(locationId)) {
            throw new IllegalArgumentException("校园地点不存在或未启用: " + locationId);
        }
    }

    private record Neighbor(long locationId, int distanceMeters) {
    }

    private record NodeDistance(long locationId, long distance) {
    }
}
