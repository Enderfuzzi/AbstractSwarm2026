import java.util.*;
import java.util.stream.Collectors;

public class PathCalculation {


    private static final HashMap<StationType, HashMap<StationType, Integer>> PATH_COST = new HashMap<>();
    private static final HashMap<StationType, Double> MEAN_PATH_COST = new HashMap<>();

    private static int max_path_cost = 1;

    record PathPair(StationType station, Integer cost) implements Comparable<PathPair> {
        @Override
        public int compareTo(PathPair other) {
            return Integer.compare(this.cost, other.cost);
        }
    };

    public static void initPathCost(List<Station> stations) {
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

        // calculate the mean distance to each reachable station
        for (StationType sourceType : stationsTypes) {
            int value = 0;
            for (Map.Entry<StationType, Integer> target : PATH_COST.get(sourceType).entrySet()) {
                value += target.getValue();
                max_path_cost = Math.max(max_path_cost, target.getValue());
            }
            MEAN_PATH_COST.put(sourceType, (double) (value / PATH_COST.get(sourceType).size()));
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
                // System.out.printf("Path calculation: %s to %s with cost: %d \n", current.name, target.name, currentPair.cost);
                return currentPair.cost;
            }

            for (PlaceEdge edge : currentPair.station.placeEdges) {
                queue.add(new PathPair((StationType) edge.connectedType, currentPair.cost + edge.weight));
            }
        }
        return -1;
    }

    public static int getPathCost(Station current, Station target) {
        if (!PATH_COST.containsKey(current.type)) return 0;
        return PATH_COST.get(current.type).getOrDefault(target.type, 0);
    }

}
