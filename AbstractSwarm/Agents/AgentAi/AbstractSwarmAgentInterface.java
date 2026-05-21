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

	private static final TimeStatistic timeStatistic = new TimeStatistic();

	private static Network model;

	private static final HashMap<Agent, List<Double>> PREVIOUS_REWARDS = new HashMap<>();


	private static final HashMap<Agent, Workprediction> WORKPREDICTION = new HashMap<>();
	private static final HashMap<Station, Set<Agent>> STATION_TARGETED = new HashMap<>();

	private static final HashMap<String, Integer> MAX_VALUES = new HashMap<>();

	/**
	 * This method allows an agent to perceive its current state and to perform
	 * actions by returning an evaluation value for potential next target
	 * stations. The method is called for every station that can be visted by
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

		if (timeStatistic.firstRun) {
			timeStatistic.firstRun = false;
			model = new Network(DataObject.getDataSize());
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

		}

		timeStatistic.currentTime = time;

		if (timeStatistic.currentTime == 1 && timeStatistic.lastTime != 1) {
			timeStatistic.newRun = true;
			timeStatistic.numberOfSimulations++;
			model.save();
			model.resetEpsilon();
			AGENT_VISITS.clear();
			STATION_VISITS.clear();

		} else {
			timeStatistic.newRun = false;
		}

		if (!PREVIOUS_REWARDS.containsKey(me)) {
			PREVIOUS_REWARDS.put(me, new ArrayList<>());
			PREVIOUS_REWARDS.get(me).add(1.0);
		}

		if (timeStatistic.firstIteration && timeStatistic.currentTime == 1) {
			// in each first time step init all path cost since an agent might not reach each station
			initPathCost(stations);



		}

		int stationTargeted = STATION_TARGETED.getOrDefault(station, new HashSet<>()).size();

		int pathCost = getPathCost(me.previousTarget, station);

		int maxAgentWorkTime = getMaxWorkTime(me);
		int remainingAgentWorkTime = getRemainingWorkTime(me);

		int maxStationWorkTime = getMaxWorkTime(station);
		int remainingStationWorkTime = getRemainingWorkTime(station);

		System.out.printf("Agent: %s, Max Work Time: %d, current Work Time: %d%n",
				me.name, maxAgentWorkTime, remainingAgentWorkTime);
		System.out.printf("Station: %s, Max work Time: %d, current Work time: %d%n",
				station.name, maxStationWorkTime, remainingStationWorkTime);

		boolean isAllowed = true;

		if (station.type.space < me.type.size) {
			isAllowed = false;
		}



		DataObject data = new DataObject.DataFactory()
				.add(DataObject.Attribute.TIME, Math.max((double) timeStatistic.currentTime / timeStatistic.bestTime, 0.0))
				.add(DataObject.Attribute.AGENT_NAME, me.name)
				.add(DataObject.Attribute.AGENT_SPACE, (double) Math.abs(me.type.size) / MAX_VALUES.get("AGENT_SPACE"))
				.add(DataObject.Attribute.AGENT_SIZE, (double) me.type.components.size() / MAX_VALUES.get("AGENT_SIZE"))
				.add(DataObject.Attribute.AGENT_FREQUENCY, (double) Math.max(me.frequency, 0) / MAX_VALUES.get("AGENT_FREQUENCY"))
				.add(DataObject.Attribute.AGENT_NECESSITY, (double) Math.max(me.type.necessity, 0) / MAX_VALUES.get("AGENT_NECESSITY"))

				.add(DataObject.Attribute.STATION_NAME, station.name)
				.add(DataObject.Attribute.STATION_SPACE, (double) station.space / MAX_VALUES.get("STATION_SPACE"))
				.add(DataObject.Attribute.STATION_SIZE, (double) station.type.components.size() / MAX_VALUES.get("STATION_SIZE"))
				.add(DataObject.Attribute.STATION_FREQUENCY, (double) Math.max(station.frequency, 0) / MAX_VALUES.get("STATION_FREQUENCY"))
				.add(DataObject.Attribute.STATION_NECESSITY, (double) Math.max(station.type.necessity, 0) / MAX_VALUES.get("STATION_NECESSITY"))
				// .add(DataObject.Attribute.STATIONS, stations)
				.add(DataObject.Attribute.PATH_COST, -1 * ((double) pathCost / max_path_cost))
				.add(DataObject.Attribute.MEAN_PATH_COST, -1 * MEAN_PATH_COST.get(station.type) / max_path_cost)

				.add(DataObject.Attribute.MAX_AGENT_WORK, (double) maxAgentWorkTime / max_agent_time)
				.add(DataObject.Attribute.MAX_STATION_WORK, (double) maxStationWorkTime / max_station_time)

				.add(DataObject.Attribute.REMAINING_AGENT_WORK, (double) remainingAgentWorkTime / max_agent_time)
				.add(DataObject.Attribute.REMAINING_STATION_WORK, (double) remainingStationWorkTime / max_station_time)

				.create();

		// System.out.println(data);

		//System.out.println(data);
		System.out.println(Arrays.toString(data.getData()));

		double result = model.predict(me, data.getData(), isAllowed);


		// double result = -1 * getPathCost(me.previousTarget, station);
		Prediction prediction = new Prediction(time, me, station, result);
		System.out.println(prediction);



		return result;
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

		if (!STATION_TARGETED.containsKey(target)) {
			STATION_TARGETED.put(target, new HashSet<>());
		}

		STATION_TARGETED.get(target).add(me);

		if (!WORKPREDICTION.containsKey(me)) {
			WORKPREDICTION.put(me, new Workprediction(me, target , (Long) defaultData[1]));
		}

		System.out.printf("[%d] Agent: %s target: %s arrival: %d %n", time, me.name, ((Station) defaultData[0]).name, (Long) defaultData[1]);

		int maxAgentWorkTime = getMaxWorkTime(me);
		int remainingAgentWorkTime = getRemainingWorkTime(me);

		System.out.printf("Agent: %s, Max Work Time: %d, current Work Time: %d%n",
				me.name, maxAgentWorkTime, remainingAgentWorkTime);



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
		System.out.printf("[%d] reward: %f \n", time, value);

        WORKPREDICTION.remove(me);

		//double lastReward = PREVIOUS_REWARDS.get(me).get(PREVIOUS_REWARDS.get(me).size() - 1);
		//double manipulatedReward = lastReward - value;

		//System.out.printf("[%d] manipulated reward: %f \n", time, manipulatedReward);

		//PREVIOUS_REWARDS.get(me).add(value);

		AGENT_VISITS.put(me, AGENT_VISITS.getOrDefault(me, 0) + 1);
		Station visited = me.target;
		System.out.println("Visited station: " + visited.name);
		STATION_VISITS.put(visited, STATION_VISITS.getOrDefault(visited, 0) + 1);

		if (finished(me, others.keySet(), stations)) {
			timeStatistic.bestTime = timeStatistic.bestTime == -1 ? time : Math.min(timeStatistic.bestTime, time);
		}

		long currentBest = timeStatistic.bestTime == -1 ? time : timeStatistic.bestTime;
		System.out.println("Current best: " + timeStatistic.bestTime);
		model.train(me, value, currentBest, time);


	}

	record Workprediction(Agent agent, Station station, long arrivalTime, long finishTime) {
		public Workprediction(Agent agent, Station station, long arrivalTime) {
			this(agent, station, arrivalTime, arrivalTime + timeAtStation(agent, station));
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



	// ----------------------------------------------
	// Path calculations

	private static final HashMap<StationType, HashMap<StationType, Integer>> PATH_COST = new HashMap<>();
	private static final HashMap<StationType, Double> MEAN_PATH_COST = new HashMap<>();

	private static int max_path_cost = 1;

	record PathPair(StationType station, Integer cost) implements Comparable<PathPair> {
		@Override
		public int compareTo(PathPair other) {
			return Integer.compare(this.cost, other.cost);
		}
	};

	private static void initPathCost(List<Station> stations) {
		// get list of all types present here
		Set<StationType> stationsTypes = stations.stream().map(station -> station.type).collect(Collectors.toSet());

		// calculate each combination
		for (StationType sourceType : stationsTypes) {
			if (PATH_COST.containsKey(sourceType)) continue;
			PATH_COST.put(sourceType, new HashMap<>());
			for (StationType targetType : stationsTypes) {
				if (sourceType == targetType) continue;
				if (PATH_COST.get(sourceType).containsKey(targetType)) continue;
				PATH_COST.get(sourceType).put(targetType, dijkstra(sourceType, targetType));
			}
		}

		// calculate the mean distance to each reachable station
		for (StationType sourceType : stationsTypes) {
			int value = 0;
			for (Map.Entry<StationType, Integer> target : PATH_COST.get(sourceType).entrySet()) {
				value += target.getValue();
				max_path_cost = Math.max(max_path_cost, target.getValue());
			}
			MEAN_PATH_COST.put(sourceType, (double) (value / PATH_COST.get(sourceType).size()));
		}

	}

	private static int dijkstra(StationType current, StationType target) {
		if (current == target) return 0;

		PriorityQueue<PathPair> queue = new PriorityQueue<>();
		queue.add(new PathPair(current, 0));
		List<StationType> used = new ArrayList<>();
		while(!queue.isEmpty()) {
			PathPair currentPair = queue.poll();
			if (used.contains(currentPair.station)) continue;
			used.add(currentPair.station);
			if (currentPair.station == target) {
				System.out.printf("Path calculation: %s to %s with cost: %d \n", current.name, target.name, currentPair.cost);
				return currentPair.cost;
			}

			for (PlaceEdge edge : currentPair.station.placeEdges) {
				queue.add(new PathPair((StationType) edge.connectedType, currentPair.cost + edge.weight));
			}
		}
		return -1;
	}

	private static int getPathCost(Station current, Station target) {
		if (!PATH_COST.containsKey(current.type)) return 0;
		return PATH_COST.get(current.type).getOrDefault(target.type, 0);
	}

	// ----------------------------------------------
	//

	private static boolean finished(Agent agent, Set<Agent> agents, List<Station> stations) {
		boolean finished = true;
		if (getRemainingWorkTime(agent) != 0) return false;
		for (Agent a : agents) {
			if (getRemainingWorkTime(a) != 0) return false;
		}

		for (Station station : stations) {
			if (getRemainingWorkTime(station) != 0) return false;
		}

		return true;
	}

	public static int timeAtStation(AgentType agent, StationType station) {
		return Math.max(agent.time, station.time);
	}

	public static int timeAtStation(Agent agent, Station station) {
		return timeAtStation(agent, station.type);
	}

	public static int timeAtStation(Agent agent, StationType station) {
		return timeAtStation(agent.type, station);
	}

	private static int timeAtStation(AgentType agentType, Station station) {
		return timeAtStation(agentType, station.type);
	}


	public static int occupationAtArrival(Agent me, Station station, int arrivalTime) {
		// TODO for each entry in WORK filter if arrival is after arrivalTime
		return 0;
	}

	// TODO calculate the working time of an agent -> frequency and necessity
	// TODO calculate the minimum waiting time?
	// TODO calculate the working time left?

	// TODO plan?

	private static final HashMap<AgentType, Integer> MAX_AGENT_WORK_TIME = new HashMap<>();

	private static int max_agent_time = 1;

	private static int getMaxWorkTime(Agent agent) {
		if (!MAX_AGENT_WORK_TIME.containsKey(agent.type)) calculateMaxWorkTime(agent);
		for (Integer value : MAX_AGENT_WORK_TIME.values()) {
			max_agent_time = Math.max(max_agent_time, value);
		}
		return MAX_AGENT_WORK_TIME.get(agent.type);
	}

	private static void calculateMaxWorkTime(Agent agent) {
		AgentType agentType = agent.type;
		if (MAX_AGENT_WORK_TIME.containsKey(agentType)) return;

		int result = 0;
		int lowestStationTime = Integer.MAX_VALUE;

		int frequencyResult = 0;
		int numberOfVisits = 0;

		for (VisitEdge edge : agentType.visitEdges) {
			if (!(edge.connectedType instanceof StationType stationType)) continue;

			int timeAtStation = timeAtStation(agent, stationType);

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

	private static int getMaxWorkTime(Station station) {
		if (!MAX_STATION_WORK_TIME.containsKey(station.type)) calculateMaxWorkTime(station);
		for (Integer value : MAX_STATION_WORK_TIME.values()) {
			max_station_time = Math.max(max_station_time, value);
		}
		return MAX_STATION_WORK_TIME.get(station.type);
	}

	private static void calculateMaxWorkTime(Station station) {
		StationType stationType = station.type;
		if (MAX_STATION_WORK_TIME.containsKey(stationType)) return;

		int result = 0;
		int lowestAgentTime = Integer.MAX_VALUE;


		int frequencyResult = 0;
		int numberOfVisits = 0;

		for (VisitEdge edge : stationType.visitEdges) {
			if (!(edge.connectedType instanceof AgentType agentType)) continue;


			int timeAtStation = timeAtStation(agentType, stationType);

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

	private static final HashMap<Agent, Integer> AGENT_VISITS = new HashMap<>();
	private static final HashMap<Station, Integer> STATION_VISITS = new HashMap<>();

	private static int getRemainingWorkTime(Agent agent) {
		AgentType agentType = agent.type;

		int result = 0;
		int necessityVisits = 0;

		if (agentType.necessity != -1) {
			for (Map.Entry<Station, Integer> entry : agent.necessities.entrySet()) {
				int timeAtStation = timeAtStation(agent, entry.getKey());

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

					int timeAtStation = timeAtStation(agent, stationType);
					lowestStation = Math.min(lowestStation, timeAtStation);
				}

				result += visitsLeft * lowestStation;
			}
		}
		return result;
	}

	private static int getRemainingWorkTime(Station station) {
		StationType stationType = station.type;

		int result = 0;
		int necessityVisits = 0;

		if (stationType.necessity != -1) {
			for (Map.Entry<Agent, Integer> entry : station.necessities.entrySet()) {
				int timeAtStation = timeAtStation(entry.getKey(), station);

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

					int timeAtStation = timeAtStation(agentType, station);
					lowestAgent = Math.min(lowestAgent, timeAtStation);
				}

				result += visitsLeft * lowestAgent;
			}
		}
		return  result;
	}

}

