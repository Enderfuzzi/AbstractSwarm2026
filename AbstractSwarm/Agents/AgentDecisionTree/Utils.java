public class Utils {

    public static int timeAtStation(AgentType agent, StationType station) {
        return Math.max(agent.time, station.time);
    }

    public static int timeAtStation(AgentType agentType, Station station) {
        return timeAtStation(agentType, station.type);
    }
    public static int timeAtStation(Agent agent, Station station) {
        return timeAtStation(agent, station.type);
    }

    public static int timeAtStation(Agent agent, StationType station) {
        return timeAtStation(agent.type, station);
    }


    public static int getSize(Agent agent) {
        return getSize(agent.type);
    }

    public static int getSize(AgentType agentType) {
        return Math.max(agentType.size, 1);
    }

    public static int getInitSpace(Station station) {
        return getInitSpace(station.type);
    }

    public static int getInitSpace(StationType stationType) {
        if (stationType.space == -1) return Integer.MAX_VALUE;
        return stationType.space;
    }
}
