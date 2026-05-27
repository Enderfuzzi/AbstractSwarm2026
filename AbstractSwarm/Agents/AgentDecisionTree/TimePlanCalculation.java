import java.util.Map;

public class TimePlanCalculation {

    private static final boolean LOG = true;

    /**
     * Check if the given Station is allowed to be visited
     * @param station The station to check
     * @return true if the station can be visited else false
     */
    public static boolean allowed(Station station) {
        StationType type = station.type;
        boolean canVisit = false;


        if (type.timeEdges.isEmpty()) return true;
        int visited = 0;
        for (TimeEdge edge : type.timeEdges) {
            // check only edges to stations here
            if (!(edge.connectedType instanceof StationType stationType)) continue;
            // skip edges which are directed
            if (edge.incoming != edge.outgoing) continue;
            //TODO
            visited++;


            boolean allowed = false;
            for (Station station1 : stationType.components) {
                if (visitsLeft(station1)) {
                    allowed = true;
                    break;
                }
            }
            // skip this station type if no station can be visited
            if (!allowed) continue;


            // check if any agent can visit the station
            for (VisitEdge visitEdge : stationType.visitEdges) {
                AgentType agentType = (AgentType) visitEdge.connectedType;
                for (Agent agent : agentType.components) {
                    if (LOG) System.out.printf("[TimePlanCalculation] %s can be visited: %b by place plan %n", stationType.name, PlacePlanCalculation.allowed(agent, stationType));
                    if (PlacePlanCalculation.allowed(agent, stationType)) {
                        canVisit = true;
                        // TODO can be optimized
                        break;
                    }
                }
                if (canVisit) break;
            }

            if (canVisit) break;
        }
        if (LOG) System.out.printf("[TimePlanCalculation] %s can be visited: %b %n", station.name, canVisit || visited == 0);
        return canVisit || visited == 0;
    }

    /**
     * Checks if the given Station has visits left
     * @param station The Station to check
     * @return true if there is at least one visit left
     */
    public static boolean visitsLeft(Station station) {
        System.out.printf("[VisitsLeft]: frequency: %d \n", station.frequency);
        if (station.type.frequency != -1) {
            if (station.frequency > 0) return true;
        }
        if (station.type.necessity != -1) {
            for (Map.Entry<Agent, Integer> entry : station.necessities.entrySet()) {
                System.out.printf("[VisitsLeft] necessity %s %d\n", entry.getKey().name, entry.getValue());
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
                            System.out.printf("[VisitsLeft] necessity %s %d\n", entry.getKey().name, entry.getValue());
                            return true;
                        }
                    }
                }
            }

        }

        return false;
    }
}
