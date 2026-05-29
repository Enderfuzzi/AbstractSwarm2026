import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to check if an agent is allowed to visit a given station. Uses only place edges as constraints.
 * @author Ole Brenner
 * @version  25.05.2026
 */
public class PlacePlanCalculation {

    private static final boolean LOG = false;

    // which stations have to be visited before the given station can be visited?
    // StationType can be visited only if -> List<StationType> has been visited
    private static final HashMap<StationType, List<StationType>> STATION_ORDER = new HashMap<>();

    private static final HashMap<StationType, List<StationType>> STATION_ORDER_REVERSE = new HashMap<>();

    // Store for each AgentType which stations are visitable
    private static final HashMap<AgentType, Set<String>> CAPABILITIES = new HashMap<>();
    private static boolean directedEdge = false;

    /**
     * Init method to insert scenario structure has should not be called more than once
     * @param agents all agents in the scenario
     * @param stations all stations in the scenario
     */
    public static void init(List<Agent> agents, List<Station> stations) {
        for (Agent agent : agents) {
            AgentType agentType = agent.type;
            if (CAPABILITIES.containsKey(agentType)) continue;
            Set<String> result = new HashSet<>();
            for (VisitEdge edge : agentType.visitEdges) {
                if (!(edge.connectedType instanceof StationType stationType)) continue;
                result.add(stationType.name);
            }
            CAPABILITIES.put(agentType, result);
        }

        // first iterate through all stations and filter all stations which have to be visited before another station
        for (Station station : stations) {
            if (STATION_ORDER.containsKey(station.type)) continue;
            List<StationType> incomingResult = new ArrayList<>();
            List<StationType> outgoingResult = new ArrayList<>();
            for (PlaceEdge edge : station.type.placeEdges) {
                StationType s = (StationType) edge.connectedType;

                // skip edges which are not incoming
                if (edge.incoming){
                    directedEdge = true;
                    incomingResult.add(s);
                }


                if (edge.outgoing) {
                    directedEdge = true;
                    outgoingResult.add(s);
                }
            }
            if (LOG) System.out.printf("[PlacePlanCalculation]: %s: incoming %s%n", station.type.name, incomingResult.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            if (LOG) System.out.printf("[PlacePlanCalculation]: %s: outgoing %s%n", station.type.name, outgoingResult.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            STATION_ORDER.put(station.type, incomingResult);
            STATION_ORDER_REVERSE.put(station.type, outgoingResult);
        }

        if (LOG) System.out.printf("[PlacePlanCalculation] has directed edges: %b%n", directedEdge);
    }


    /**
     * List to track the visited stations per agent
     */
    public static final HashMap<Agent, List<Station>> VISITED = new HashMap<>();

    /**
     * Adds a visit to the list
     * @param agent The agent which visited
     * @param station The station visited
     */
    public static void addVisit(Agent agent, Station station) {
        if (LOG) System.out.printf("[PlacePlanCalculation]: Add Visit: %s %s\n", agent.name, station.name);
        if (!VISITED.containsKey(agent)) VISITED.put(agent, new ArrayList<>());
        VISITED.get(agent).add(station);
    }

    /**
     * Resets the list of visited stations
     */
    public static void reset() {
        VISITED.clear();
    }

    /**
     * Checks if the agent is allowed to visit a station type with directed place edges
     * @param agent The Agent for the visit
     * @param type The station type to check
     * @return true if the type is allowed or false if not
     */
    public static boolean allowed(Agent agent, StationType type) {
        // case there is no directed edge in the scenario
        if (!directedEdge) return true;

        return checkNextVisit(agent, type) && checkLastVisit(agent, type);
    }

    public static boolean allowed(Agent agent, Station station) {
        return allowed(agent, station.type);
    }

    private static boolean checkLastVisit(Agent agent, StationType type) {
        if (!VISITED.containsKey(agent) || VISITED.get(agent).isEmpty()) return true;

        List<Station> visits = VISITED.get(agent);
        // if (LOG) System.out.printf("[PlacePlanCalculation]: %s %s \n", type.name, visits.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
        Station lastVisit = visits.get(visits.size() - 1);
        StationType stationTypeKey = lastVisit.type;
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s last visited station: %s \n", type.name, stationTypeKey.name);
        // case no outgoing station

        if (!STATION_ORDER_REVERSE.containsKey(stationTypeKey)) return true;
        List<StationType> stationOrder = STATION_ORDER_REVERSE.get(stationTypeKey);
        if (stationOrder.isEmpty()) return true;

        for (StationType stationType : stationOrder) {
            // return true if the type is in the reverse list
            if (stationType == type){
                if (LOG) System.out.printf("[PlacePlanCalculation]: %s as next station allowed: true\n", type.name);
                return true;
            }
        }

        if (LOG) System.out.printf("[PlacePlanCalculation]: %s as next station allowed: false\n", type.name);
        // case the list is not empty but no match found
        return false;
    }


    private static boolean checkNextVisit(Agent agent, StationType type) {
        String stationTypeName = type.name;
        String agentTypeName = agent.type.name;
        // case there is no order for the station type
        if (!STATION_ORDER.containsKey(type)) return true;
        if (STATION_ORDER.get(type).isEmpty()) return true;
        // case station has predecessor but agent didn't visit any station yet

        // case the agent has not visited a station yet
        if (!VISITED.containsKey(agent) || VISITED.get(agent).isEmpty()) {
            // iterate through all stations which have to be visited before a station
            for (StationType stationType : STATION_ORDER.get(type)) {
                // check if the station can be visited by the agent if yes return false else return true
                if (CAPABILITIES.get(agent.type).contains(stationType.name)) {
                    if (LOG) System.out.printf("[PlacePlanCalculation]: %s can not be visited by %s\n", agent.name, type.name);
                    return false;
                }
            }
            if (LOG) System.out.printf("[PlacePlanCalculation]: %s can be visited by %s\n", agent.name, type.name);
            return true;
        }

        int counted = 0;
        List<Station> visitedStations = VISITED.get(agent);
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s Visited stations: %s%n", agent.name, visitedStations.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));

        // count number of times the target type has been visited by the agent (any station)
        int targetedVisited = 0;
        for (Station station : visitedStations) {
            if (station.type == type) targetedVisited++;
        }

        // count the visit of the agent per station type
        HashMap<String, Integer> countVisits = new HashMap<>();
        HashMap<String, Integer> countNegativeVisits = new HashMap<>();

        for (StationType stationType : STATION_ORDER.get(type)) {
            // check if the agent can visit the station type if not skip

            if (!CAPABILITIES.get(agent.type).contains(stationType.name)) continue;

            // count how often the predecessing stations have been visited
            for (Station s : visitedStations) {
                if (s.type == stationType) {
                    countVisits.put(s.name, countVisits.getOrDefault(s.name, 0) + 1);
                } else {
                    countNegativeVisits.put(s.name, countVisits.getOrDefault(s.name, 0) + 1);
                }
            }
        }
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s Visits: %s \n", stationTypeName, countVisits);
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s Negativ Visits: %s \n", stationTypeName, countNegativeVisits);
        // iterate through all visits per type
        // sum them up if the number of visits of all visitable predecessors is larger than the target then the target can be visited
        int numberOfVisits = 0;
        for (Integer visitNumber : countVisits.values()) {
            numberOfVisits += visitNumber;
        }
        int negativeVisits = 0;
        for (Integer value : countNegativeVisits.values()) negativeVisits += value;

        boolean isAllowed = targetedVisited < numberOfVisits - negativeVisits;
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s target visits: %d sum of visits: %d \n", stationTypeName, targetedVisited, numberOfVisits - negativeVisits);
        if (LOG) System.out.printf("[PlacePlanCalculation]: %s is allowed: %b \n", stationTypeName, isAllowed);

        return isAllowed;
    }
}
