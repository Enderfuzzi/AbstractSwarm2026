/*
AbstractSwarm agent that makes random decisions
Copyright (C) 2020  Daan Apeldoorn (daan.apeldoorn@uni-mainz.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.util.*;
import java.util.stream.Collectors;


/**
 * This class provides the three methods of the AbstractSwarm agent interface 
 * for state perception and performing actions, communication with other agents
 * and the perception of rewards.<br />
 * <br />
 * 
 * The properties of the interface's objects are:<br />
 * <br />
 * 
 * AGENT:<br />
 * .name           the agent's name<br />
 * .type           the agent's AGENT_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the agent's type has
 *                 no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected station,
 *                 -1 if the agent's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if agent's type has no cycle attribute<br />
 * .time           the remaining time on the target station, -1 if agent is
 *                 currently not visiting a station or if agent's type has no
 *                 time attribute<br />
 * .target         the agent's current target<br />
 * .visiting       whether the agent is currently visiting a station<br />
 * <br />
 * 
 * STATION:<br />
 * .name           the station's name<br />
 * .type           the station's STATION_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the station's type
 *                 has no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected agent,
 *                 -1 if the station's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if station's type has no cycle attribute<br />
 * .space          the remaining space, -1 if the station's type has no space
 *                 attribute<br />
 * <br />
 * 
 * AGENT_TYPE:<br />
 * .name           the agent type's name as string<br />
 * .type           the type as string ("AGENT_TYPE")<br />
 * .components     the agent type's AGENTs (see above)<br />
 * .frequency      the agent type's frequency attribute; -1 if the agent type 
 *                 has no frequency attribute<br />
 * .necessity      the agent type's necessity attribute; -1 if the agent type 
 *                 has no necessity attribute<br />          
 * .cycle          the agent type's cycle attribute; -1 if agent type has no
 *                 cycle attribute<br />
 * .time           the agent type's time attribute, -1 if agent type has no
 *                 time attribute<br />
 * .size           the agent type's size attribute, -1 if agent type has no
 *                 size attribute<br />
 * .priority       the agent type's priority attribute, -1 if agent type has
 *                 no priority attribute<br />
 * .visitEdges     the agent type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the agent type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the agent type's PLACE_EDGEs as list (see below)
 *                 (note that, by definition, agent types cannot have place
 *                 edges and thus this list will always be empty; this is only
 *                 for backwards compatibility/unification reasons)<br />
 * <br />
 *
 * STATION_TYPE:<br />
 * .name           the station type's name as string<br />
 * .type           the type as string ("STATION_TYPE")<br />
 * .components     the station type's stations<br />
 * .frequency      the station type's frequency attribute; -1 if the station
 *                 type has no frequency attribute<br />
 * .necessity      the station type's necessity attribute; -1 if the station
 *                 type has no necessity attribute<br />          
 * .cycle          the station type's cycle attribute; -1 if station type has
 *                 no cycle attribute<br />
 * .time           the station type's time attribute, -1 if station type has no
 *                 time attribute<br />
 * .space          the station type's space attribute, -1 if station type has
 *                 no space attribute<br />
 * .visitEdges     the station type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the station type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the station type's PLACE_EDGEs as list (see below)<br />
 * <br />
 *
 * VISIT EDGE:<br />
 * .type           the edge's type as string ("VISIT_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .bold           whether the edge is bold<br />
 * <br />
 * 
 * TIME EDGE:<br />
 * .type           the edge's type as string ("TIME_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 * .andConnected   whether the edge is and-connected to the opposite type<br />
 * .andOrigin      whether the edge is and-connected at its origin type<br />
 * <br />
 * 
 * PLACE EDGE:<br />
 * .type         the edge's type as string ("PLACE_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 */
public class AbstractSwarmAgentInterface {

	private static final boolean LOG = true;
	private static final TimeStatistic timeStatistic = new TimeStatistic();

	private static final HashMap<Agent, List<Double>> PREVIOUS_REWARDS = new HashMap<>();


	private static final HashMap<Agent, Workprediction> WORKPREDICTION = new HashMap<>();
	private static final HashMap<Station, Set<Agent>> STATION_TARGETED = new HashMap<>();

	private static final HashMap<String, Integer> MAX_VALUES = new HashMap<>();

	private static final HashMap<String, Probabilities> WEIGHTS = new HashMap<>();

	private static String lastAgent = "";

	private static int counter = 0;

	private static final HashMap<String, Station> VISIT = new HashMap<>();

	/**
	 * This method allows an agent to perceive its current state and to perform
	 * actions by returning an evaluation value for potential next target
	 * stations. The method is called for every station that can be visited by
	 * the agent. 
	 * 
	 * @param me        the agent itself
	 * @param others    all other agents in the scenario with their currently
	 *                  communicated information
	 * @param stations  all stations in the scenario
	 * @param time      the current time unit
	 * @param station   the station to be evaluated as potential next target
	 * 
	 * @return          the evaluation value of the station
	 */
	public static double evaluation( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station ) {

		AgentType agentType = me.type;
		String weightKey = me.name;

		if (timeStatistic.firstRun) {
			timeStatistic.firstRun = false;
			// model.load();
			// TODO put first init here

			for (Station s : stations) {
				MAX_VALUES.put("STATION_SPACE", Math.max(MAX_VALUES.getOrDefault("STATION_SPACE", 1), s.space));
				MAX_VALUES.put("STATION_SIZE", Math.max(MAX_VALUES.getOrDefault("STATION_SIZE", 1), s.type.components.size()));
				MAX_VALUES.put("STATION_NECESSITY", Math.max(MAX_VALUES.getOrDefault("STATION_NECESSITY", 1), s.type.necessity));
				MAX_VALUES.put("STATION_FREQUENCY", Math.max(MAX_VALUES.getOrDefault("STATION_FREQUENCY", 1), s.type.frequency));
			}

			MAX_VALUES.put("AGENT_SPACE", Math.max(MAX_VALUES.getOrDefault("AGENT_SPACE", 1), me.type.size));
			MAX_VALUES.put("AGENT_SIZE", Math.max(MAX_VALUES.getOrDefault("AGENT_SIZE", 1), me.type.components.size()));
			MAX_VALUES.put("AGENT_NECESSITY", Math.max(MAX_VALUES.getOrDefault("AGENT_NECESSITY", 1), me.type.necessity));
			MAX_VALUES.put("AGENT_FREQUENCY", Math.max(MAX_VALUES.getOrDefault("AGENT_FREQUENCY", 1), me.type.frequency));

			for (Agent a : others.keySet()) {
				MAX_VALUES.put("AGENT_SPACE", Math.max(MAX_VALUES.getOrDefault("AGENT_SPACE", 1), a.type.size));
				MAX_VALUES.put("AGENT_SIZE", Math.max(MAX_VALUES.getOrDefault("AGENT_SIZE", 1), a.type.components.size()));
				MAX_VALUES.put("AGENT_NECESSITY", Math.max(MAX_VALUES.getOrDefault("AGENT_NECESSITY", 1), a.type.necessity));
				MAX_VALUES.put("AGENT_FREQUENCY", Math.max(MAX_VALUES.getOrDefault("AGENT_FREQUENCY", 1), a.type.frequency));
			}


			List<Agent> agents = new ArrayList<>(others.keySet());
			agents.add(me);
			PlacePlanCalculation.init(agents, stations);
			WorkCalculation.init(agents, stations);

		}

		timeStatistic.currentTime = time;


		if (timeStatistic.currentTime == 1 && timeStatistic.lastTime != 1) {
			timeStatistic.newRun = true;
			timeStatistic.numberOfSimulations++;

			PlacePlanCalculation.reset();
			SpaceCalculation.reset();

			counter++;
			if (counter % 5 == 0) {
				// WEIGHTS.clear();
			}

			Probabilities.reroll();
		} else {
			timeStatistic.newRun = false;
		}

		if (!PREVIOUS_REWARDS.containsKey(me)) {
			PREVIOUS_REWARDS.put(me, new ArrayList<>());
			PREVIOUS_REWARDS.get(me).add(1.0);
		}

		if (timeStatistic.firstIteration && timeStatistic.currentTime == 1) {
			// in each first time step init all path cost since an agent might not reach each station
			PathCalculation.init(stations);
		}

		if (!WEIGHTS.containsKey(weightKey)) {
			WEIGHTS.put(weightKey, new Probabilities());
		}


		if (!lastAgent.equals(me.type.name)) {
			//Probabilities.reroll();
			lastAgent = me.type.name;
		}


		InputData currentData = new InputData(me, others, stations, time, station);

		int stationTargeted = STATION_TARGETED.getOrDefault(station, new HashSet<>()).size();

		int pathCost = PathCalculation.getPathCost(me.previousTarget, station);

		int maxAgentWorkTime = WorkCalculation.getMaxWorkTime(me);
		int remainingAgentWorkTime = WorkCalculation.getRemainingWorkTime(me);

		int maxStationWorkTime = WorkCalculation.getMaxWorkTime(station);
		int remainingStationWorkTime = WorkCalculation.getRemainingWorkTime(station);


		// Case station is not a legal target
		if (
				Utils.getSize(me) > Utils.getInitSpace(station)
				|| !PlacePlanCalculation.allowed(me, station)
				|| !TimePlanCalculation.allowed(station)
		) return Double.NEGATIVE_INFINITY;


		// Epsilon case





		// other cases
		Probabilities probabilities = WEIGHTS.get(weightKey);

		System.out.printf("[%d] %s possible start time: %d%n", time, me.name, SpaceCalculation.nextFreeSlot(me, station, time, pathCost + time));

		System.out.printf("Visits left: %b \n", TimePlanCalculation.visitsLeft(station));

		System.out.printf("Normal: %b Time: %b \n", probabilities.checkKey(ProbabilityKey.NORMAL), probabilities.checkKey(ProbabilityKey.TIME));

		System.out.printf("Probabilities: %s \n", WEIGHTS.entrySet().stream().map((entry) -> entry.getKey() + "=" + entry.getValue().toString()).collect(Collectors.joining(" , ")));

		if (probabilities.checkKey(ProbabilityKey.NORMAL)) {
			return 1.0 / (SpaceCalculation.nextFreeSlot(me, station, time, pathCost + time) + 1);
		}

		if (probabilities.checkKey(ProbabilityKey.TIME)) {
			double timeEdge = SpaceCalculation.timeEdgeCalculation(me, station, time, pathCost + time);
			double directedEdge = SpaceCalculation.directedTimeEdgeCalculation(me, station, time, pathCost + time);
			return timeEdge + (directedEdge * (1 - timeEdge));
		}

		/*
		double[] result = new double[4];

		result[3] = (double) (WorkCalculation.getRemainingWorkTime(me, station)) / WorkCalculation.getMaxWorkTime(me, station);

		System.out.printf("[%d] Work time current: %d Work time max: %d \n", time, WorkCalculation.getRemainingWorkTime(me, station), WorkCalculation.getMaxWorkTime(me, station));

		// last calculation
		timeStatistic.lastTime = time;
		return 1.0;

		 */
		timeStatistic.lastTime = time;
		return 1.0;
	}
	
	
	/**
	 * This method allows an agent to communicate with other agents by
	 * returning a communication data object.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param defaultData  a triple (selected station, time unit when the 
	 *                     station is reached, evaluation value of the station)
	 *                     that can be used for default communication
	 * 
	 * @return             the communication data object
	 */
	public static Object communication( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData ) {
		Station target = (Station) defaultData[0];
		long arrival = (Long) defaultData[1];
		double value = (Double) defaultData[2];

		VISIT.put(me.name, target);

		if (!STATION_TARGETED.containsKey(target)) {
			STATION_TARGETED.put(target, new HashSet<>());
		}

		STATION_TARGETED.get(target).add(me);

		if (!WORKPREDICTION.containsKey(me)) {
			WORKPREDICTION.put(me, new Workprediction(me, target , (Long) defaultData[1]));
		}

		if (LOG) System.out.printf("[%d] Communication | Agent: %s target: %s arrival: %d %n", time, me.name, target.name, arrival);

		int maxAgentWorkTime = WorkCalculation.getMaxWorkTime(me);
		int remainingAgentWorkTime = WorkCalculation.getRemainingWorkTime(me);

		// System.out.printf("Agent: %s, Max Work Time: %d, current Work Time: %d%n",
		//		me.name, maxAgentWorkTime, remainingAgentWorkTime);


		SpaceCalculation.update(me, target, arrival, value);

		return defaultData;
	}

	
	/**
	 * This method allows an agent to perceive the local reward for its most
	 * recent action.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param value        the local reward in [0, 1] for the agent's most
	 *                     recent action 
	 */
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value ) {
		System.out.printf("[%d] Reward | %f \n", time, value);

		Station visited = VISIT.remove(me.name);


        WORKPREDICTION.remove(me);

		//double lastReward = PREVIOUS_REWARDS.get(me).get(PREVIOUS_REWARDS.get(me).size() - 1);
		//double manipulatedReward = lastReward - value;

		//System.out.printf("[%d] manipulated reward: %f \n", time, manipulatedReward);

		//PREVIOUS_REWARDS.get(me).add(value);

		WorkCalculation.increaseVisits(me);
		WorkCalculation.increaseVisits(visited);

		if (LOG) System.out.printf("[%d] target station: %s previous station: %s%n", time, null, me.previousTarget.name);


		if (finished(me, others.keySet(), stations)) {
			timeStatistic.bestTime = timeStatistic.bestTime == -1 ? time : Math.min(timeStatistic.bestTime, time);
		}

		long currentBest = timeStatistic.bestTime == -1 ? time : timeStatistic.bestTime;
		// System.out.println("Current best: " + timeStatistic.bestTime);

		SpaceCalculation.finishedVisit(me);
		PlacePlanCalculation.addVisit(me, visited);


		WEIGHTS.get(me.name).increase(value);
	}

	record Workprediction(Agent agent, Station station, long arrivalTime, long finishTime) {
		public Workprediction(Agent agent, Station station, long arrivalTime) {
			this(agent, station, arrivalTime, arrivalTime + Utils.timeAtStation(agent, station));
		}
	}

	// ----------------------------------------------
	// Time statistics

	static class TimeStatistic {
			public boolean firstRun = true;
			public boolean newRun = true;

			public long currentTime = 0L;

			public long lastTime = 0L;

			public long bestTime = -1L;

			public boolean firstIteration = true;

			public long numberOfSimulations = 0L;
	}

	enum ProbabilityKey {
		NORMAL(0.5),
		TIME(0.5)

		;

		public final double initValue;
		ProbabilityKey(double initValue) {
			this.initValue = initValue;
		}
	}

	record ProbPair(ProbabilityKey key, double weight){}

	static class Probabilities {

		private final List<ProbPair> probabilities = new ArrayList<>();


		private static final Random RANDOM = new Random();

		private static double current = RANDOM.nextDouble();

		public Probabilities(ProbabilityKey[] keys, Double[] probabilities) {
			for (int i = 0; i < keys.length; i++) {
				this.probabilities.add(new ProbPair(keys[i],probabilities[i]));
			}
			normalize();
		}

		public Probabilities() {
			for(ProbabilityKey key : ProbabilityKey.values()) {
				probabilities.add(new ProbPair(key, key.initValue));
			}
			normalize();
		}

		public static void reroll() {
			current = RANDOM.nextDouble();
		}

		public void normalize() {
			double sum = 0;
			for (ProbPair pair : probabilities) sum += pair.weight;
			if (sum == 0) return;
			final double used_sum = sum;
			probabilities.replaceAll(pair -> new ProbPair(pair.key,pair.weight / used_sum));
		}

		public boolean checkKey(ProbabilityKey key) {
			return getKey() == key;
		}

		public ProbabilityKey getKey() {
			double sum = 0;
			for (ProbPair pair : probabilities) {
				sum += pair.weight;
				if (current <= sum) return pair.key;
			}
			return ProbabilityKey.NORMAL;
		}

		private int getIndex(ProbabilityKey key) {
			for (int i = 0; i < probabilities.size(); i++) {
				if (probabilities.get(i).key == key) return i;
			}
			return 0;
		}

		public void increase(double value) {
			ProbabilityKey key = getKey();
			int index = getIndex(key);
			ProbPair old = probabilities.remove(index);
			probabilities.add(index, new ProbPair(key,  Math.min(0.05, Math.max(old.weight + value, 0.95))));
			normalize();
		}

		@Override
		public String toString() {
			return probabilities.stream()
					.map(ProbPair::toString)
					.collect(Collectors.joining(", "));
		}
	}

	// ----------------------------------------------
	// Path calculations



	// ----------------------------------------------
	//

	private static boolean finished(Agent agent, Set<Agent> agents, List<Station> stations) {
		boolean finished = true;
		if (WorkCalculation.getRemainingWorkTime(agent) != 0) return false;
		for (Agent a : agents) {
			if (WorkCalculation.getRemainingWorkTime(a) != 0) return false;
		}

		for (Station station : stations) {
			if (WorkCalculation.getRemainingWorkTime(station) != 0) return false;
		}

		return true;
	}




	public static int occupationAtArrival(Agent me, Station station, int arrivalTime) {
		// TODO for each entry in WORK filter if arrival is after arrivalTime
		return 0;
	}

	// TODO calculate the working time of an agent -> frequency and necessity
	// TODO calculate the minimum waiting time?
	// TODO calculate the working time left?

	// TODO plan?



	private static Double[] normalize(Double[] weights) {
		double sum = 0;
		for (double value : weights) sum += value;
		if (sum == 0) return weights;

		Double[] result = new Double[weights.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = weights[i] / sum;
		}
		return result;
	}

	private static final double LEARNING_RATE = 0.95;
	private static Double[] rewardCalculation(long time, double reward, Double[] weights) {
		double timePenalty = reward / Math.log10(time + 1);

		Double[] result = new Double[weights.length];
		for(int i = 0; i < result.length; i++) {
			result[i] = weights[i] + LEARNING_RATE * timePenalty * (weights[i] - 0.5);
			result[i] = Math.max(0.0, Math.min(1.0, result[i]));
		}

		return normalize(result);
	}

}

