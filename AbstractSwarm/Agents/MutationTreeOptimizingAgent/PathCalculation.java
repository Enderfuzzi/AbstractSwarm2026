import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides the distance between two station types
 *
 * @author Ole Brenner
 * @version 25.05.2026
 */
public class PathCalculation {

    private static final boolean LOG = true;

    private static final HashMap<StationType, HashMap<StationType, Integer>> PATH_COST = new HashMap<>();

    record PathPair(StationType station, Integer cost) implements Comparable<PathPair> {
        @Override
        public int compareTo(PathPair other) {
            return Integer.compare(this.cost, other.cost);
        }
    }

    /**
     * Calculates and stores the path to each station to improve calculation time
     * @param stations all stations in the scenario
     */
    public static void init(List<Station> stations) {
        // get list of all types present here
        Set<StationType> stationsTypes = stations.stream().map(station -> station.type).collect(Collectors.toSet());

        // calculate each combination
        for (StationType sourceType : stationsTypes) {
            if (PATH_COST.containsKey(sourceType)) continue;
            PATH_COST.put(sourceType, new HashMap<>());
            for (StationType targetType : stationsTypes) {
                if (sourceType == targetType) continue;
                if (PATH_COST.get(sourceType).containsKey(targetType)) continue;
                PATH_COST.get(sourceType).put(targetType, dijkstra(sourceType, targetType));
            }
        }
    }

    private static int dijkstra(StationType current, StationType target) {
        if (current == target) return 0;

        PriorityQueue<PathPair> queue = new PriorityQueue<>();
        queue.add(new PathPair(current, 0));
        List<StationType> used = new ArrayList<>();
        while(!queue.isEmpty()) {
            PathPair currentPair = queue.poll();
            if (used.contains(currentPair.station)) continue;
            used.add(currentPair.station);
            if (currentPair.station == target) {
                if (LOG) System.out.printf("Path calculation: %s to %s with cost: %d \n", current.name, target.name, currentPair.cost);
                return currentPair.cost;
            }

            for (PlaceEdge edge : currentPair.station.placeEdges) {
                queue.add(new PathPair((StationType) edge.connectedType, currentPair.cost + edge.weight));
            }
        }
        return -1;
    }

    /**
     *
     * @param current the start station
     * @param target the target station
     * @return the distance or -1 if no connection is available
     */
    public static int getPathCost(Station current, Station target) {
        return getPathCost(current.type, target.type);
    }

    /**
     *
     * @param current the start station type
     * @param target the target station type
     * @return the distance or -1 if no connection is available
     */
    public static int getPathCost(StationType current, StationType target) {
        if (!PATH_COST.containsKey(current)) return -1;
        return PATH_COST.get(current).getOrDefault(target, -1);
    }

    /**
     * Checks if a target station is reachable given a start station
     * @param current the start station
     * @param target the target station
     * @return whether the target station is reachable from the start station
     */
    public static boolean reachable(Station current, Station target) {
        return getPathCost(current, target) != -1;
    }

}
