import java.util.*;
import java.util.stream.Collectors;

public class TimePlanCalculation {

    private static final boolean LOG = false;

    private static final HashMap<StationType, List<StationType>> SIMULTANEOUS_VISITS = new HashMap<>();

    private static final HashMap<StationType, List<StationType>> STATION_ORDER = new HashMap<>();

    private static final HashMap<StationType, List<StationType>> AND_STATION_ORDER= new HashMap<>();

    private static final HashMap<StationType, List<StationType>> STATION_ORDER_REVERSE = new HashMap<>();

    // Store for each AgentType which stations are visitable
    private static final HashMap<AgentType, Set<String>> CAPABILITIES = new HashMap<>();

    private static boolean hasTimeEdges = false;

    public static void init(List<Agent> agents, List<Station> stations) {
        for (Station station : stations) {
            if (STATION_ORDER.containsKey(station.type)) continue;
            List<StationType> incomingResult = new ArrayList<>();
            List<StationType> undirectedResult = new ArrayList<>();

            List<StationType> andIncomingResult = new ArrayList<>();
            List<StationType> andUndirectedResult = new ArrayList<>();

            for (TimeEdge edge : station.type.timeEdges) {
                hasTimeEdges = true;
                if (!(edge.connectedType instanceof StationType stationType)) continue;

                if (edge.incoming) {
                    if (edge.andOrigin) andIncomingResult.add(stationType);
                    else incomingResult.add(stationType);
                }

                if (edge.incoming == edge.outgoing) {
                    undirectedResult.add(stationType);
                }

            }
            if (LOG) System.out.printf("[TimePlanCalculation]: %s: incoming %s%n", station.type.name, incomingResult.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            if (LOG) System.out.printf("[TimePlanCalculation]: %s: undirected %s%n", station.type.name, undirectedResult.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            if (LOG) System.out.printf("[TimePlanCalculation]: %s: and incoming %s%n", station.type.name, andIncomingResult.stream().map(s -> s.name).collect(Collectors.joining(", ", "[", "]")));
            STATION_ORDER.put(station.type, incomingResult);
            SIMULTANEOUS_VISITS.put(station.type, undirectedResult);
            AND_STATION_ORDER.put(station.type, andIncomingResult);
        }
        // checks only if there are time edges between agents
        for (Agent agent : agents) {
            if (hasTimeEdges) break;
            for (TimeEdge edge : agent.type.timeEdges) {
                hasTimeEdges = true;
                break;
            }
        }

    }



    /**
     * Check if the given Station is allowed to be visited. Returns true without any check if there are no time edges in the scenario
     * @param station The station to check
     * @return true if the station can be visited else false
     */
    public static boolean allowed(Station station, HashMap<Agent, Object> others) {
        if (!hasTimeEdges) return true;
        return checkUndirectedEdge(station)
                && checkDirectedEdge(station, others)
                && checkAndDirectedEdge(station, others);
    }

    /**
     * Checks if the given Station has visits left
     * @param station The Station to check
     * @return true if there is at least one visit left
     */
    public static boolean visitsLeft(Station station) {
        if (LOG) System.out.printf("[VisitsLeft]: frequency: %d \n", station.frequency);
        if (station.type.frequency != -1) {
            if (station.frequency > 0) return true;
        }
        if (station.type.necessity != -1) {
            for (Map.Entry<Agent, Integer> entry : station.necessities.entrySet()) {
                if (LOG) System.out.printf("[VisitsLeft] necessity %s %d\n", entry.getKey().name, entry.getValue());
                if (entry.getValue() > 0) return true;
            }
        }

        for (VisitEdge edge : station.type.visitEdges) {
            AgentType agentType = (AgentType) edge.connectedType;
            if (agentType.necessity == -1 && agentType.frequency == -1) return false;
            for (Agent agent : agentType.components) {
                if (agentType.frequency != -1) {
                    if (agent.frequency > 0) return true;
                }
                if (agentType.necessity != -1) {
                    for (Map.Entry<Station, Integer> entry : agent.necessities.entrySet()) {
                        if (!entry.getKey().equals(station)) continue;
                        if (entry.getValue() > 0) {
                            if (LOG) System.out.printf("[VisitsLeft] necessity %s %d\n", entry.getKey().name, entry.getValue());
                            return true;
                        }
                    }
                }
            }

        }

        return false;
    }


   public static boolean checkUndirectedEdge(Station station) {
        StationType type = station.type;
        if (!SIMULTANEOUS_VISITS.containsKey(type)) return true;
        List<StationType> stations = SIMULTANEOUS_VISITS.get(type);
        if (stations.isEmpty()) return true;

        for (StationType stationType : stations) {

            boolean allowed = false;
            for (Station station1 : stationType.components) {
                if (visitsLeft(station1)) {
                    allowed = true;
                    break;
                }
            }
            // Skip this station type if no station has visits left
            if (!allowed) continue;


            for (VisitEdge visitEdge : stationType.visitEdges) {
                AgentType agentType = (AgentType) visitEdge.connectedType;
                for (Agent agent : agentType.components) {
                    if (LOG) System.out.printf("[TimePlanCalculation] %s can be visited: %b by place plan %n", stationType.name, PlacePlanCalculation.allowed(agent, stationType));
                    if (PlacePlanCalculation.allowed(agent, stationType)) {
                        // At least one Station of this type can be visited
                        return true;
                    }
                }
            }

        }

        return false;
    }

    // Station Type name, number of visits
    public static final HashMap<StationType, Integer> VISITED = new HashMap<>();

    public static void addVisit(Agent agent, Station station) {
        if (LOG) System.out.printf("[TimePlanCalculation]: Add Visit: %s %s\n", agent.name, station.name);
        VISITED.put(station.type, VISITED.getOrDefault(station.type, 0) + 1);
    }

    public static void reset() {
        VISITED.clear();
    }

    public static boolean checkDirectedEdge(Station station, HashMap<Agent, Object> others) {
        StationType type = station.type;

        if (!STATION_ORDER.containsKey(type)) return true;
        List<StationType> stations = STATION_ORDER.get(type);
        if (stations.isEmpty()) return true;

        int numberOfVisits = 0;
        for (StationType stationType : stations) {
            numberOfVisits += VISITED.getOrDefault(stationType, 0);

            // add the number of agents targeting the station
            numberOfVisits += Calculations.stationTargeted(others, station);
        }

        int numberOfVisitsAtTarget = VISITED.getOrDefault(station.type, 0);

        if (LOG) System.out.printf("[TimePlanCalculation] directed number of visits: %d at target: %d \n", numberOfVisits, numberOfVisitsAtTarget);

        boolean allowed = numberOfVisits > numberOfVisitsAtTarget;

        if (LOG) System.out.printf("[TimePlanCalculation] directed: %s %b \n", station.name, allowed);

        return allowed;
    }


    public static boolean checkAndDirectedEdge(Station station, HashMap<Agent, Object> others) {
        StationType type = station.type;

        if (!AND_STATION_ORDER.containsKey(type)) return true;
        List<StationType> stations = AND_STATION_ORDER.get(type);
        if (stations.isEmpty()) return true;


        HashMap<String, Integer> visitedByType = new HashMap<>();

        int numberOfVisitsAtTarget = VISITED.getOrDefault(station.type, 0);

        for (StationType stationType : stations) {
            visitedByType.put(stationType.name, VISITED.getOrDefault(stationType, 0) +
                    Calculations.stationTargeted(others, station));
        }

        for (Integer value : visitedByType.values()) {
            if (value <= numberOfVisitsAtTarget) {
                if (LOG) System.out.printf("[TimePlanCalculation] and directed: %s false \n", station.name);
                return false;
            }
        }

        if (LOG) System.out.printf("[TimePlanCalculation] and directed: %s true \n", station.name);
        return true;
    }
}
