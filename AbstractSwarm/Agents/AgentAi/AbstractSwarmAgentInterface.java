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
			// TODO put first init here
		}

		timeStatistic.currentTime = time;

		if (timeStatistic.currentTime == 1 && timeStatistic.lastTime != 1) {
			timeStatistic.newRun = true;
			timeStatistic.numberOfSimulations++;

			model.resetEpsilon();

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

		DataObject data = new DataObject.DataFactory()
				.add(DataObject.Attribute.TIME, timeStatistic.currentTime)
				.add(DataObject.Attribute.AGENT_NAME, me.name)
				.add(DataObject.Attribute.AGENT_SPACE, Math.abs(me.type.size))
				.add(DataObject.Attribute.AGENT_SIZE, me.type.components.size())
				.add(DataObject.Attribute.AGENT_FREQUENCY, Math.max(me.frequency, 0))
				.add(DataObject.Attribute.AGENT_NECESSITY, Math.max(me.type.necessity, 0))

				.add(DataObject.Attribute.STATION_NAME, station.name)
				.add(DataObject.Attribute.STATION_SPACE, station.space)
				.add(DataObject.Attribute.STATION_SIZE, station.type.components.size())
				.add(DataObject.Attribute.STATION_FREQUENCY, Math.max(station.frequency, 0))
				.add(DataObject.Attribute.STATION_NECESSITY, Math.max(station.type.necessity, 0))
				// .add(DataObject.Attribute.STATIONS, stations)
				.add(DataObject.Attribute.PATH_COST, -1 * pathCost)
				.add(DataObject.Attribute.MEAN_PATH_COST, -1 * MEAN_PATH_COST.get(station.type))
				.create();

		// System.out.println(data);

		// System.out.println(data.getHeader());
		System.out.println(Arrays.toString(data.getData()));

		double result = model.predict(me, data.getData());


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




		return null;
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

		model.train(me, value);
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

			public boolean firstIteration = true;

			public long numberOfSimulations = 0L;
	}



	// ----------------------------------------------
	// Path calculations

	private static final HashMap<StationType, HashMap<StationType, Integer>> PATH_COST = new HashMap<>();
	private static final HashMap<StationType, Double> MEAN_PATH_COST = new HashMap<>();

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
				// System.out.printf("Path calculation: %s to %s with cost: %d \n", current.name, target.name, currentPair.cost);
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

	public static int timeAtStation(Agent agent, Station station) {
		return Math.max(agent.time, station.type.time);
	}

	public static int occupationAtArrival(Agent me, Station station, int arrivalTime) {
		// TODO for each entry in WORK filter if arrival is after arrivalTime
		return 0;
	}

	// TODO calculate the working time of an agent -> frequency and necessity
	// TODO calculate the minimum waiting time?
	// TODO calculate the working time left?

}
