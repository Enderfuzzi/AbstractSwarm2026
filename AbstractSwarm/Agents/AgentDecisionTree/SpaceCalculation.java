import java.util.*;

public class SpaceCalculation {

    // Station name as key
    private static final HashMap<String, PriorityQueue<TimeTuple>> OCCUPATION = new HashMap<>();
    // Agent name as key
    private static final HashMap<String, TimeTuple> AGENT_LOOKUP = new HashMap<>();
    private static final HashMap<String, Station> STATION_LOOKUP = new HashMap<>();

    private static final boolean LOG = false;

    public static void update(Agent agent, Station station, long arrivalTime, double stationValue) {
        String agentKey = agent.name;
        String stationKey = station.name;

        if (!OCCUPATION.containsKey(stationKey)) OCCUPATION.put(stationKey, new PriorityQueue<>());
        // delete old tuple
        if (AGENT_LOOKUP.containsKey(agentKey) && STATION_LOOKUP.containsKey(agentKey)) {
            PriorityQueue<TimeTuple> queue = OCCUPATION.get(STATION_LOOKUP.get(agentKey).name);
            queue.remove(AGENT_LOOKUP.get(agentKey));
        }

        long finishTime = arrivalTime + Utils.timeAtStation(agent, station);
        TimeTuple newTuple = new TimeTuple(agent, arrivalTime, finishTime, stationValue);
        PriorityQueue<TimeTuple> queue = OCCUPATION.get(stationKey);
        queue.add(newTuple);


        OCCUPATION.put(stationKey, queue);
        AGENT_LOOKUP.put(agentKey, newTuple);
        STATION_LOOKUP.put(agentKey, station);

        if (LOG) System.out.println("stationKey: " + station.name + " " + OCCUPATION.get(stationKey));

    }

    public static void finishedVisit(Agent agent) {
        if (!AGENT_LOOKUP.containsKey(agent.name) || !STATION_LOOKUP.containsKey(agent.name)) return;
        Station station = STATION_LOOKUP.remove(agent.name);
        TimeTuple tuple = AGENT_LOOKUP.remove(agent.name);

        if (!OCCUPATION.containsKey(station.name)) return;
        OCCUPATION.get(station.name).remove(tuple);
    }

    private static final PriorityQueue<TimeTuple> CURRENT_VISITS = new PriorityQueue<>(Comparator.comparingLong(TimeTuple::finishTime));

    public static long nextFreeSlot(Agent agent, Station station, long time, long arrival) {
        if (LOG) System.out.println("NEXT FREE SLOT: " + station.name);
        // clear occupation list
        CURRENT_VISITS.clear();
        PriorityQueue<TimeTuple> queue = OCCUPATION.get(station.name);

        if (LOG) System.out.println(queue);

        if (queue == null || queue.isEmpty()) return arrival;
        // if the station has no space attribute there is infinite space
        int capacity = station.type.space == -1 ? Integer.MAX_VALUE : station.type.space;
        int usedCapacity = 0;
        long clock = time;

        // copy current list
        PriorityQueue<TimeTuple> copy = new PriorityQueue<>(OCCUPATION.get(station.name));

        // create the possible time tuple to put in list of current agent
        long timeAtStation = Utils.timeAtStation(agent, station);
        TimeTuple imaginary = new TimeTuple(agent, arrival, arrival + timeAtStation, 0);
        copy.add(imaginary);

        // System.out.printf("Copy Queue: %s%n", copy);

        while(!copy.isEmpty()) {
            TimeTuple current = copy.poll();
            int size = Math.max(current.agent.type.size, 1);

            if (LOG) System.out.println("Current: " + current);
            // remove agents which are finished before arrival
            while (!CURRENT_VISITS.isEmpty() && CURRENT_VISITS.peek().finishTime <= current.arrivalTime) {
                TimeTuple toRemove = CURRENT_VISITS.poll();
                usedCapacity -= toRemove.agent.type.size;
            }

            if (LOG) System.out.printf("Capacity: %d used capacity: %d%n", capacity, usedCapacity);

            // remove agents, need to wait, skip time until capacity is free
            while (usedCapacity + size > capacity && !CURRENT_VISITS.isEmpty()) {
                TimeTuple toRemove = CURRENT_VISITS.poll();
                usedCapacity -= toRemove.agent.type.size;
                clock = Math.max(clock, toRemove.finishTime);
            }
            // case capacity is free
            long actualStart = Math.max(clock, current.arrivalTime);
            long visitTime = current.finishTime - current.arrivalTime;

            if (current.agent.name.equals(agent.name)) {
                if (LOG) System.out.println("RESULT: " + actualStart);
                return actualStart;
            }

            CURRENT_VISITS.add(new TimeTuple(current.agent, actualStart, actualStart + visitTime, current.stationValue));
            usedCapacity += size;
            //clock = actualStart;
        }
        if (LOG) System.out.println("RESULT: " + Math.max(clock, arrival));
        return Math.max(clock, arrival);
    }


    public static double timeEdgeCalculation(Agent agent, Station station, long time, long arrival) {
        // TODO muss gefixt werden
        double multiplier = WorkCalculation.getScenarioMaxWorktime();
        long longTime = Utils.timeAtStation(agent, station);
        double stationTime = Math.log10(longTime + 1.0) / Math.log10(longTime + 1.0 + multiplier);

        // TODO Hat die Station Verbundene Zeit Kanten?
        if (station.type.timeEdges.isEmpty()) return stationTime;

        // TODO Wie oft wird die aktuelle Station targeted
        // copy of current occupation of the station
        PriorityQueue<TimeTuple> stationOccupation = OCCUPATION.containsKey(station.name) ? new PriorityQueue<>(OCCUPATION.get(station.name)) : new PriorityQueue<>();
        // remove entries before current time
        while (!stationOccupation.isEmpty() && stationOccupation.peek().arrivalTime < time) stationOccupation.poll();

        PriorityQueue<TimeTuple> occupationToMatch = new PriorityQueue<>();

        for (TimeEdge edge : station.type.timeEdges) {
            // TODO zuerst Kanten ohne Richtung (gewicht immer 0)
            if (edge.incoming != edge.outgoing) continue;

            if (edge.connectedType instanceof StationType stationType) {
                for (Station currentStation : stationType.components) {
                    if (!OCCUPATION.containsKey(currentStation.name)) continue;
                    PriorityQueue<TimeTuple> currentOccupation = new PriorityQueue<>(OCCUPATION.get(currentStation.name));
                    // Skip if occupation is empty
                    if (currentOccupation.isEmpty()) continue;
                    // skip visits which arrival time is less than time (the visits have begun)
                    while (!currentOccupation.isEmpty() && currentOccupation.peek().arrivalTime < time) currentOccupation.poll();

                    // add these visits to the list
                    occupationToMatch.addAll(currentOccupation);
                }
            }

            if (edge.connectedType instanceof AgentType agentType) {
                for (Agent currentAgent : agentType.components) {
                    // check if the agent has a target
                    if (!AGENT_LOOKUP.containsKey(currentAgent.name)) continue;
                    if (AGENT_LOOKUP.get(currentAgent.name).arrivalTime < time) continue;

                    occupationToMatch.add(AGENT_LOOKUP.get(currentAgent.name));
                }
            }
        }

        long workTimeLeft = 0L;

        // stationOccupation.add(new TimeTuple(agent, arrival, arrival + Utils.timeAtStation(agent, station), 0));



        for (TimeTuple partnerVisit : occupationToMatch) {
            long minDistance = Long.MAX_VALUE;

            for (TimeTuple target : stationOccupation) {
                long distance = Math.abs(target.arrivalTime - partnerVisit.arrivalTime);
                minDistance = Math.min(minDistance, distance);
            }

            if (minDistance == Long.MAX_VALUE) continue;
            if (minDistance > 0) {
                long partnerDuration = partnerVisit.finishTime - partnerVisit.arrivalTime;

                workTimeLeft += partnerDuration + (minDistance);
            }
        }



        /*
        while (!stationOccupation.isEmpty() && !occupationToMatch.isEmpty()) {
            // match each visit
            stationOccupation.poll();
            occupationToMatch.poll();
        }
        */

        // if occupationToMatch is not empty then this station should be targeted else rather not
        // higher value if station should be targeted
        // lower if not

        if (workTimeLeft == 0L || occupationToMatch.isEmpty()) {
            System.out.printf("Station Time weight: %f \n", stationTime);
            return stationTime;
        }

        while (!occupationToMatch.isEmpty()) {
            TimeTuple tuple = occupationToMatch.poll();
            workTimeLeft += (tuple.finishTime - tuple.arrivalTime);
        }
        double timeFactor = Math.log10(workTimeLeft + 1) / Math.log10((workTimeLeft + multiplier));
        // scale to [0, 1.0]

        System.out.printf("Station Time weight: %f \n", stationTime + (timeFactor * (1.0 - stationTime)));

        return stationTime + (timeFactor * (1.0 - stationTime));
    }


    public static double directedTimeEdgeCalculation(Agent agent, Station station, long time, long arrival) {
        double multiplier = WorkCalculation.getScenarioMaxWorktime();
        long longTime = Utils.timeAtStation(agent, station);
        double stationTime = Math.log10(longTime + 1.0) / Math.log10(longTime + 1.0 + multiplier);

        if (station.type.timeEdges.isEmpty()) return stationTime;

        double result = 0;

        List<DirectedTuple> occupationToMatch = new ArrayList<>();
        PriorityQueue<TimeTuple> stationOccupation = OCCUPATION.containsKey(station.name) ? new PriorityQueue<>(OCCUPATION.get(station.name)) : new PriorityQueue<>();

        int counter = 0;

        for (TimeEdge edge : station.type.timeEdges) {
            // TODO Kanten mit Richtung
            if (edge.incoming == edge.outgoing) continue;


            // TODO erstmal nur Stationen
            int weight = edge.weight;
            if (edge.connectedType instanceof StationType stationType) {
                int worktime  = WorkCalculation.getMaxWorkTimeForStation(stationType);

                result += Math.log10(worktime + weight) / Math.log10((worktime + multiplier));

                for (Station currentStation : stationType.components) {
                    if (!OCCUPATION.containsKey(currentStation.name)) continue;
                    PriorityQueue<TimeTuple> currentOccupation = new PriorityQueue<>(OCCUPATION.get(currentStation.name));

                    if (currentOccupation.isEmpty()) continue;
                    while (!currentOccupation.isEmpty() && currentOccupation.peek().arrivalTime < time - weight) currentOccupation.poll();

                    // add these visits to the list
                    for (TimeTuple tuple : currentOccupation) {
                        occupationToMatch.add(new DirectedTuple(tuple,edge));
                    }
                    // occupationToMatch.addAll(currentOccupation);
                }

                counter++;
            }

            if (edge.connectedType instanceof AgentType agentType) {
                int worktime  = WorkCalculation.getMaxWorkTimeForAgent(agentType);

                result += Math.log10(worktime + weight) / Math.log10((worktime + multiplier));

                counter++;
            }


        }

        long workTimeLeft = 0L;
        if (counter == 0) return stationTime;
        result = result / counter;

        for (DirectedTuple tuple : occupationToMatch) {
            long minDistance = Long.MAX_VALUE;

            for (TimeTuple target : stationOccupation) {
                long penalty = 0;
                if (tuple.edge.outgoing) {
                    long start = target.arrivalTime + tuple.edge.weight;
                    if (tuple.tuple.arrivalTime < start) {
                        penalty = start - tuple.tuple.arrivalTime;
                    }
                } else {
                    long start = tuple.tuple.arrivalTime + tuple.edge.weight;
                    if (target.arrivalTime < start) {
                        penalty = start - target.arrivalTime;
                    }

                }
                minDistance = Math.min(minDistance, penalty);

                /*
                long distance = Math.abs(target.arrivalTime - partnerVisit.arrivalTime);
                minDistance = Math.min(minDistance, distance);

                 */
            }
            if (minDistance == Long.MAX_VALUE) continue;
            if (minDistance > 0) {
                long duration = tuple.tuple.finishTime - tuple.tuple.arrivalTime;

                // Die Distanz wirkt als Skalierungsfaktor: Je größer die Lücke, desto drastischer der Strafwert
                workTimeLeft += duration + (minDistance * minDistance * minDistance);
            }
        }

        double resultBase = stationTime + (result * (1.0 - stationTime));

        if (workTimeLeft > 0) {
            double value = Math.log10(workTimeLeft + 1) / Math.log10(workTimeLeft + 1.0 + multiplier);
            return resultBase + (value * (1.0 - resultBase));
        }

        return resultBase;
    }


    public static void reset() {
        OCCUPATION.clear();
        AGENT_LOOKUP.clear();
        STATION_LOOKUP.clear();
    }

    record DirectedTuple(TimeTuple tuple, TimeEdge edge) {

    }


    record TimeTuple(
            Agent agent,
            long arrivalTime,
            long finishTime,
            double stationValue
    ) implements Comparable<TimeTuple> {

        @Override
        public int compareTo(TimeTuple tuple) {
            // first arrival time
            int comparison = Long.compare(arrivalTime, tuple.arrivalTime());
            // then station value
            if (comparison == 0) comparison = Double.compare(tuple.stationValue(), stationValue);
            // and then finish time
            if (comparison == 0) comparison = Double.compare(tuple.finishTime(), finishTime);
            if (comparison == 0) comparison = agent.name.compareTo(tuple.agent.name);
            return comparison;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (!(other instanceof TimeTuple tuple)) return false;
            return agent.name.equals(tuple.agent().name);
        }

        @Override
        public int hashCode() {
            return agent.name.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s: arrival: %d finish: %d value: %f", agent.name, arrivalTime, finishTime, stationValue);
        }
    }


}
