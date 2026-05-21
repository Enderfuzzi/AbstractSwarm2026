import java.util.ArrayList;
import java.util.List;

public class TimePlanCalculation {

    private static final boolean LOG = true;

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
                if (WorkCalculation.visitsLeft(station1)) {
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


    public static double directedEdgeCalculation(Agent agent, Station station) {
        double result = 0;
        int number = 0;

        List<TimeEdge> edges = station.type.timeEdges;
        // case outgoing
        // TODO Station hat ausgehende Kante -> gewicht der Kante + Zeit an der Station

        for (TimeEdge edge : edges) {
            int weight = edge.weight;
            if (edge.outgoing) {


            }

            if (edge.incoming) {

            }

        }


        // case incoming

        return result;
    }

}
