import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkCalculation {

    private static final HashMap<AgentType, Integer> MAX_AGENT_WORK_TIME = new HashMap<>();

    private static int max_agent_time = 1;

    public static void reset() {
        AGENT_VISITS.clear();
        STATION_VISITS.clear();
    }

    public static void init(List<Agent> agentList, List<Station> stationList) {
        for (Agent agent : agentList) {
            calculateMaxWorkTime(agent);

            AGENT_VISITS.put(agent.name, 0);
        }
        for (Station station : stationList) {
            calculateMaxWorkTime(station);

            int visits = 0;
            if (station.type.necessity != -1) {
                for (int value : station.necessities.values()) visits += value;
            }
            if (station.type.frequency != -1) visits += station.frequency;


            for (VisitEdge edge : station.type.visitEdges) {
                if (!(edge.connectedType instanceof AgentType agentType)) continue;

                if (agentType.frequency != -1) visits += agentType.frequency;
                if (agentType.necessity != -1) visits += agentType.necessity;
            }
            MAX_STATION_VISITS.put(station.name, visits);

            STATION_VISITS.put(station.name, 0);
        }


    }

    public static int getMaxWorkTime(Agent agent) {
        if (!MAX_AGENT_WORK_TIME.containsKey(agent.type)) calculateMaxWorkTime(agent);
        for (Integer value : MAX_AGENT_WORK_TIME.values()) {
            max_agent_time = Math.max(max_agent_time, value);
        }
        return MAX_AGENT_WORK_TIME.get(agent.type);
    }

    private static void calculateMaxWorkTime(Agent agent) {
        calculateMaxWorkTime(agent.type);
    }

    private static void calculateMaxWorkTime(AgentType agentType) {
        if (MAX_AGENT_WORK_TIME.containsKey(agentType)) return;

        int result = 0;
        int lowestStationTime = Integer.MAX_VALUE;

        int frequencyResult = 0;
        int numberOfVisits = 0;

        for (VisitEdge edge : agentType.visitEdges) {
            if (!(edge.connectedType instanceof StationType stationType)) continue;

            int timeAtStation = Utils.timeAtStation(agentType, stationType);

            if (agentType.necessity != -1) {
                result += timeAtStation
                        * stationType.components.size()
                        * agentType.necessity;

                numberOfVisits += stationType.components.size() * agentType.necessity;
            }

            if (agentType.frequency != -1) {
                if (lowestStationTime > timeAtStation) {
                    lowestStationTime = timeAtStation;
                    frequencyResult = timeAtStation * agentType.frequency;
                }
            }

        }
        // case both attributes are present

        if (agentType.necessity != -1 && agentType.frequency != -1) {
            if (numberOfVisits < agentType.frequency) {
                // add missing visits
                result += (agentType.frequency - numberOfVisits) * lowestStationTime;
            }
        }

        if (agentType.necessity != -1) {
            MAX_AGENT_WORK_TIME.put(agentType, result);
        } else if (agentType.frequency != -1) {
            MAX_AGENT_WORK_TIME.put(agentType, frequencyResult);
        } else {
            MAX_AGENT_WORK_TIME.put(agentType, 0);
        }
    }

    private static final HashMap<StationType, Integer> MAX_STATION_WORK_TIME = new HashMap<>();

    private static int max_station_time = 1;

    public static int getMaxWorkTime(Station station) {
        if (!MAX_STATION_WORK_TIME.containsKey(station.type)) calculateMaxWorkTime(station);
        for (Integer value : MAX_STATION_WORK_TIME.values()) {
            max_station_time = Math.max(max_station_time, value);
        }
        return MAX_STATION_WORK_TIME.get(station.type);
    }

    private static void calculateMaxWorkTime(Station station) {
        calculateMaxWorkTime(station.type);
    }

    private static void calculateMaxWorkTime(StationType stationType) {
        if (MAX_STATION_WORK_TIME.containsKey(stationType)) return;

        int result = 0;
        int lowestAgentTime = Integer.MAX_VALUE;


        int frequencyResult = 0;
        int numberOfVisits = 0;

        for (VisitEdge edge : stationType.visitEdges) {
            if (!(edge.connectedType instanceof AgentType agentType)) continue;


            int timeAtStation = Utils.timeAtStation(agentType, stationType);

            if (stationType.necessity != -1) {
                result += timeAtStation
                        * agentType.size
                        * stationType.necessity;

                numberOfVisits += agentType.size * stationType.necessity;
            }

            if (stationType.frequency != -1) {
                if (lowestAgentTime > timeAtStation) {
                    lowestAgentTime = timeAtStation;
                    frequencyResult = timeAtStation * stationType.frequency;
                }
            }
        }

        if (stationType.necessity != -1 && stationType.frequency != -1) {
            if (numberOfVisits < stationType.frequency) {
                // add missing visits
                result += (stationType.frequency - numberOfVisits) * lowestAgentTime;
            }
        }

        if (stationType.necessity != -1) {
            MAX_STATION_WORK_TIME.put(stationType, result);
        } else if (stationType.frequency != -1) {
            MAX_STATION_WORK_TIME.put(stationType, frequencyResult);
        } else {
            MAX_STATION_WORK_TIME.put(stationType, 0);
        }
    }

    public static int getMaxWorkTime(Agent agent, Station station) {
        return Math.max(getMaxWorkTime(agent), getMaxWorkTime(station));
    }


    public static int getScenarioMaxWorktime() {
        int maxTimeValue = 0;
        for (Integer value : MAX_AGENT_WORK_TIME.values()) {
            maxTimeValue = Math.max(maxTimeValue, value);
        }

        for (Integer value : MAX_STATION_WORK_TIME.values()) {
            maxTimeValue = Math.max(maxTimeValue, value);
        }
        return maxTimeValue;
    }

    public static int getMaxWorkTimeForStation(StationType stationType) {
        int highest = 0;
        for (VisitEdge edge : stationType.visitEdges) {
            if (!(edge.connectedType instanceof AgentType agentType)) continue;

            highest = Math.max(highest, MAX_AGENT_WORK_TIME.get(agentType));
        }
        return Math.max(highest, MAX_STATION_WORK_TIME.get(stationType));

        /*
        int result = 0;
        int frequencyResult = 0;

        for (VisitEdge edge : station.type.visitEdges) {
            AgentType agentType = (AgentType) edge.connectedType;

            int timeAtStation = Utils.timeAtStation(agentType, station.type);

            if (agentType.necessity != -1) {
                result += timeAtStation * agentType.size * agentType.necessity;
            }

            if (agentType.frequency != -1) {
                StationType lowestType = station.type;

                for (VisitEdge visitEdge : agentType.visitEdges) {
                    StationType currentType = (StationType) visitEdge.connectedType;

                    if (Utils.timeAtStation(agentType, currentType) < timeAtStation) {
                        lowestType = currentType;
                        break;
                    }
                }

                if (lowestType == station.type) {
                    frequencyResult = timeAtStation * agentType.frequency;
                }
            }
        }

        return Math.max(getMaxWorkTime(station), result + frequencyResult);

         */
    }

    public static int getMaxWorkTimeForAgent(AgentType agentType) {
        int lowest = 0;
        for (VisitEdge edge : agentType.visitEdges) {
            if (!(edge.connectedType instanceof StationType stationType)) continue;

            lowest = Math.max(lowest, MAX_STATION_WORK_TIME.get(stationType));
        }

        return Math.max(lowest, MAX_AGENT_WORK_TIME.get(agentType));
    }

    // station.name as key
    private static final HashMap<String, Integer> MAX_STATION_VISITS = new HashMap<>();


    private static final HashMap<String, Integer> AGENT_VISITS = new HashMap<>();
    private static final HashMap<String, Integer> STATION_VISITS = new HashMap<>();

    public static void increaseVisits(Agent agent) {
        AGENT_VISITS.put(agent.name, AGENT_VISITS.getOrDefault(agent.name, 0) + 1);
    }

    public static void increaseVisits(Station station) {
        STATION_VISITS.put(station.name, STATION_VISITS.getOrDefault(station.name, 0) + 1);
    }


    public static int getRemainingWorkTime(Agent agent) {
        AgentType agentType = agent.type;

        int result = 0;
        int necessityVisits = 0;

        if (agentType.necessity != -1) {
            for (Map.Entry<Station, Integer> entry : agent.necessities.entrySet()) {
                int timeAtStation = Utils.timeAtStation(agent, entry.getKey());

                result += timeAtStation * entry.getValue();
                necessityVisits += entry.getValue();
            }
        }

        if (agentType.frequency != -1) {
            int visitsLeft = agentType.frequency - AGENT_VISITS.getOrDefault(agent, 0);
            if (visitsLeft > necessityVisits) {
                // find lowestStation
                int lowestStation = Integer.MAX_VALUE;


                for (VisitEdge edge : agentType.visitEdges) {
                    if (!(edge.connectedType instanceof StationType stationType)) continue;

                    int timeAtStation = Utils.timeAtStation(agent, stationType);
                    lowestStation = Math.min(lowestStation, timeAtStation);
                }

                result += visitsLeft * lowestStation;
            }
        }
        return result;
    }

    public static int getRemainingWorkTime(Station station) {
        StationType stationType = station.type;

        int result = 0;
        int necessityVisits = 0;

        if (stationType.necessity != -1) {
            for (Map.Entry<Agent, Integer> entry : station.necessities.entrySet()) {
                int timeAtStation = Utils.timeAtStation(entry.getKey(), station);

                result += timeAtStation * entry.getValue();
                necessityVisits += entry.getValue();
            }
        }

        if (stationType.frequency != -1) {
            int visitsLeft = stationType.frequency - STATION_VISITS.getOrDefault(station, 0);
            if (visitsLeft > necessityVisits) {
                int lowestAgent = Integer.MAX_VALUE;

                for (VisitEdge edge : stationType.visitEdges) {
                    if (!(edge.connectedType instanceof AgentType agentType)) continue;

                    int timeAtStation = Utils.timeAtStation(agentType, station);
                    lowestAgent = Math.min(lowestAgent, timeAtStation);
                }

                result += visitsLeft * lowestAgent;
            }
        }
        return  result;
    }


    public static int getRemainingWorkTime(Agent agent, Station station) {
        return Math.max(getRemainingWorkTime(agent), getRemainingWorkTime(station));
    }


}
