import java.util.*;

/**
 * For calculation when the next time slot is free for a given station
 * @author Ole Brenner
 * @version 27.05.2026
 */
public class SpaceCalculation {

    // Station name as key
    private static final HashMap<Station, PriorityQueue<TimeTuple>> OCCUPATION = new HashMap<>();
    // Agent name as key
    private static final HashMap<Agent, TimeTuple> AGENT_LOOKUP = new HashMap<>();
    private static final HashMap<Agent, Station> STATION_LOOKUP = new HashMap<>();

    private static final boolean LOG = false;

    /**
     * Updates the current agent targets. Should be called in every communication-Method call.
     * @param agent         The agent to update
     * @param station       The currently targeted station
     * @param arrivalTime   The arrival time when the agent will arrive at the station
     * @param stationValue  The station value for that agent
     */
    public static void update(Agent agent, Station station, long arrivalTime, double stationValue) {
        if (!OCCUPATION.containsKey(station)) OCCUPATION.put(station, new PriorityQueue<>());
        // delete old tuple
        if (AGENT_LOOKUP.containsKey(agent) && STATION_LOOKUP.containsKey(agent)) {
            PriorityQueue<TimeTuple> queue = OCCUPATION.get(STATION_LOOKUP.get(agent));
                queue.remove(AGENT_LOOKUP.get(agent));
        }

        long finishTime = arrivalTime + Calculations.timeAtStation(agent, station.type);
        TimeTuple newTuple = new TimeTuple(agent, arrivalTime, finishTime, stationValue);
        PriorityQueue<TimeTuple> queue = OCCUPATION.get(station);
        queue.add(newTuple);


        OCCUPATION.put(station, queue);
        AGENT_LOOKUP.put(agent, newTuple);
        STATION_LOOKUP.put(agent, station);

        if (LOG) System.out.println("stationKey: " + station.name + " " + OCCUPATION.get(station));

    }

    /**
     * Has to be called when an agent finished its visit at a station
     * @param agent The agent who visited
     */
    public static void finishedVisit(Agent agent) {
        if (!AGENT_LOOKUP.containsKey(agent) || !STATION_LOOKUP.containsKey(agent)) return;
        Station station = STATION_LOOKUP.remove(agent);
        TimeTuple tuple = AGENT_LOOKUP.remove(agent);

        if (!OCCUPATION.containsKey(station)) return;
        OCCUPATION.get(station).remove(tuple);
    }

    private static final PriorityQueue<TimeTuple> CURRENT_VISITS = new PriorityQueue<>(Comparator.comparingLong(TimeTuple::finishTime));

    /**
     * Computes the time when the given agent can start a visit at the given station. The earliest visit is the arrival time of the agent.
     * Uses the stored information about other agents to keep track of the occupation of all stations.
     * @param agent     The Agent who should visit
     * @param station   The station to be visited
     * @param time      The current time step
     * @param arrival   The time when the agent can arrive at the station
     * @return          the time when the agent can visit the given station
     */
    public static long nextFreeSlot(Agent agent, Station station, long time, long arrival) {
        if (LOG) System.out.println("NEXT FREE SLOT: " + station.name);
        // clear occupation list
        CURRENT_VISITS.clear();
        PriorityQueue<TimeTuple> queue = OCCUPATION.get(station);

        if (LOG) System.out.println(queue);

        if (queue == null || queue.isEmpty()) return arrival;
        // if the station has no space attribute there is infinite space
        int capacity = station.type.space == -1 ? Integer.MAX_VALUE : station.type.space;
        int usedCapacity = 0;
        long clock = time;

        // copy current list
        PriorityQueue<TimeTuple> copy = new PriorityQueue<>(OCCUPATION.get(station));

        // create the possible time tuple to put in list of current agent
        long timeAtStation = Calculations.timeAtStation(agent, station.type);
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


    /**
     * Resets all visit lists
     */
    public static void reset() {
        OCCUPATION.clear();
        AGENT_LOOKUP.clear();
        STATION_LOOKUP.clear();
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
