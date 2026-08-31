package com.camigo.routing;

import com.camigo.entity.CampusRouteEdge;
import com.camigo.mapper.CampusLocationMapper;
import com.camigo.mapper.CampusRouteEdgeMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** 从数据库加载校园图，并缓存全部有效地点之间的最短路径。 */
@Service
public class CampusRouteNetwork implements ShortestPathProvider {

    private static final Logger log = LoggerFactory.getLogger(CampusRouteNetwork.class);

    private final CampusLocationMapper locationMapper;
    private final CampusRouteEdgeMapper edgeMapper;
    private volatile DijkstraPathFinder pathFinder;
    private volatile Map<PathKey, ShortestPath> shortestPathCache = Map.of();

    public CampusRouteNetwork(CampusLocationMapper locationMapper,
                              CampusRouteEdgeMapper edgeMapper) {
        this.locationMapper = locationMapper;
        this.edgeMapper = edgeMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpAfterStartup() {
        refresh();
        log.info("校园最短路径缓存已就绪，共 {} 条有向查询结果", shortestPathCache.size());
    }

    /** 道路或地点数据变更后调用，重新加载图并一次性重建两两路径缓存。 */
    public synchronized void refresh() {
        List<Long> locationIds = locationMapper.selectActiveLocationIds();
        List<GraphEdge> edges = edgeMapper.selectActiveEdges().stream()
                .map(CampusRouteNetwork::toGraphEdge)
                .toList();
        DijkstraPathFinder reloaded = new DijkstraPathFinder(locationIds, edges);
        Map<PathKey, ShortestPath> rebuilt = new HashMap<>();
        for (Long from : locationIds) {
            for (Long to : locationIds) {
                rebuilt.put(new PathKey(from, to), reloaded.shortestPath(from, to));
            }
        }
        pathFinder = reloaded;
        shortestPathCache = Map.copyOf(rebuilt);
    }

    /** 明确清空内存中的图和结果；下一次查询会从数据库重新加载。 */
    public synchronized void invalidate() {
        pathFinder = null;
        shortestPathCache = Map.of();
    }

    @Override
    public ShortestPath shortestPath(long fromLocationId, long toLocationId) {
        PathKey key = new PathKey(fromLocationId, toLocationId);
        ShortestPath cached = shortestPathCache.get(key);
        if (cached != null) {
            return cached;
        }
        ensureLoaded();
        cached = shortestPathCache.get(key);
        if (cached != null) {
            return cached;
        }
        return pathFinder.shortestPath(fromLocationId, toLocationId);
    }

    public int cachedPathCount() {
        return shortestPathCache.size();
    }

    private synchronized void ensureLoaded() {
        if (pathFinder == null) {
            refresh();
        }
    }

    private static GraphEdge toGraphEdge(CampusRouteEdge edge) {
        return new GraphEdge(edge.getFromLocationId(), edge.getToLocationId(),
                edge.getDistanceMeters(), Integer.valueOf(1).equals(edge.getBidirectional()));
    }

    private record PathKey(long fromLocationId, long toLocationId) {
    }
}
