import java.util.*;
import java.util.stream.Collectors;

public class PlacePlanCalculation {

    private static final boolean LOG = true;

    // which stations have to be visited before the given station can be visited?
    // <StationTypeName, List<StationType>>
    // StationType can be visited only if -> List<StationType> has been visited
    private static final HashMap<String, List<StationType>> STATION_ORDER = new HashMap<>();

    private static final HashMap<String, List<StationType>> STATION_ORDER_REVERSE = new HashMap<>();

    // TODO later usage load all agents and save connected stations for faster calculation
    private static final HashMap<String, Set<StationType>> CAPABILITIES = new HashMap<>();

    private static boolean directedEdge = false;
    public static void init(List<Station> stations) {
        for (Station station : stations) {
            if (STATION_ORDER.containsKey(station.type.name)) continue;
            List<StationType> result = new ArrayList<>();
            for (PlaceEdge edge : station.type.placeEdges) {
                // skip edges which are not incoming
                if (!edge.incoming) continue;

                StationType s = (StationType) edge.connectedType;
                result.add(s);
            }
            if (LOG) System.out.printf("[PlacePlanCalculation]: %s: %s%n", station.type.name, result.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            STATION_ORDER.put(station.type.name, result);
        }

        for (List<StationType> values : STATION_ORDER.values()) {
            if (!values.isEmpty()) {
                directedEdge = true;
                break;
            }
        }

        if (LOG) System.out.printf("[PlacePlanCalculation] has directed edges: %b%n", directedEdge);
    }


    // <Agent.name, List<Station>>
    private static final HashMap<String, List<Station>> VISITED = new HashMap<>();

    public static void addVisit(Agent agent, Station station) {
        if (!VISITED.containsKey(agent.name)) VISITED.put(agent.name, new ArrayList<>());
        VISITED.get(agent.name).add(station);
    }

    public static void reset() {
        VISITED.clear();
    }

    public static boolean allowed(Agent agent, StationType type) {
        // case there is no directed edge in the scenario
        if (!directedEdge) return true;

        String stationTypeName = type.name;
        // case there is no order for the station type
        if (!STATION_ORDER.containsKey(stationTypeName)) return true;
        if (STATION_ORDER.get(stationTypeName).isEmpty()) return true;
        // case station has predecessor but agent didn't visit any station yet

        // case the agent has not visit a station yet
        if (!VISITED.containsKey(agent.name) || VISITED.get(agent.name).isEmpty()) {
            // iterate through all stations which have to be visited
            for (StationType stationType : STATION_ORDER.get(stationTypeName)) {
                // check if the station can be visited by the agent if yes return false else return true
                for (VisitEdge edge : stationType.visitEdges) {
                    if (edge.connectedType == agent.type) {
                        return false;
                    }
                }
            }
            return true;
        }

        int counted = 0;
        List<Station> visitedStations = VISITED.get(agent.name);
        if (LOG) System.out.printf("[PlacePlanCalculation]: Visited stations: %s%n", visitedStations.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));

        // count number of times the target type has been visited by the agent (any station)
        int targetedVisited = 0;
        for (Station station : visitedStations) {
            if (station.type == type) targetedVisited++;
        }

        // count the visit of the agent per station type
        HashMap<String, Integer> countVisits = new HashMap<>();

        for (StationType stationType : STATION_ORDER.get(stationTypeName)) {
            // check if the agent can visit the station type if not skip
            boolean skip = true;
            for (VisitEdge edge : stationType.visitEdges) {
                if (edge.connectedType == agent.type) {
                    skip = false;
                    break;
                }
            }

            // number of not skipped stations that have to be visited
            if (skip) {
                counted++;
                continue;
            }

            for (Station s : visitedStations) {
                if (s.type == stationType) {
                    countVisits.put(s.name, countVisits.getOrDefault(s.name, 0) + 1);

                    counted++;
                    //break;
                }
            }
        }
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s Visits: %s \n", stationTypeName, countVisits);
        boolean isAllowed = false;
        for (Integer value : countVisits.values()) {
            if (value <= targetedVisited) {
                isAllowed = false;
                break;
            }
        }
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s is allowed: %b \n", stationTypeName, isAllowed);

        //if (LOG) System.out.printf("[PlacePlanCalculation]: %s Counted station: %d Necessary visits: %d%n", stationTypeName, counted, STATION_ORDER.get(stationTypeName).size());
        //return counted >= STATION_ORDER.get(stationTypeName).size();
        return isAllowed;
    }

    public static boolean allowed(Agent agent, Station station) {
        return allowed(agent, station.type);
    }



}
