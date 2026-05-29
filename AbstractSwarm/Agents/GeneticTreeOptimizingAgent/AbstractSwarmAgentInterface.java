/*
AbstractSwarm agent that is based on a decision tree
Copyright (C) 2020  Daan Apeldoorn (daan.apeldoorn@uni-mainz.de)
Edited by Ole Brenner (ole.brenner@stud.uni-hannover.de)

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
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
public class AbstractSwarmAgentInterface 
{

	private static Solution currentSolution = new Solution();
	
	private static FinishedSolution bestSolution;
	
	private static long round_time_unit = 0;
	
	private static TimeStatistics timeStatistic = new TimeStatistics();
	
	private static boolean firstRun = true;
	
	private static final boolean LOG = false;
	
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
	public static double evaluation(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		
		if (firstRun) {
			firstRun = false;
			currentSolution = new Solution();
			timeStatistic = new TimeStatistics();
			round_time_unit = 0;
		}
		
		timeStatistic.time = time;
		
		if (time == 1 && timeStatistic.lastValue != 1) {
			timeStatistic.numberOfRuns++;
			timeStatistic.runsSinceCurrentBest++;
			
			timeStatistic.newRun = true;
			

			if (timeStatistic.numberOfRuns == 100 && bestSolution != null) {
				if (LOG) System.out.print(bestSolution);
				if (LOG) System.out.print(bestSolution.predictedTimePlanning());
			}
			
			
			if (timeStatistic.lastRunCompleted) timeStatistic.numberOfCompletedRuns++;
			
			if (timeStatistic.lastRunCompleted) {
				if (bestSolution == null) {
					bestSolution = new FinishedSolution(currentSolution.getSolution());
				} else {
					FinishedSolution tmp = new FinishedSolution(currentSolution.getSolution());
					timeStatistic.currentTwT = tmp.calculateTWT();
					if (tmp.calculateTWT() < bestSolution.calculateTWT()) {
						bestSolution = tmp;
						timeStatistic.newBestRun = true;
						timeStatistic.lowestTwT = tmp.calculateTWT();
						if (timeStatistic.roundTimeUnit > 0) {
							timeStatistic.lowestTimeUnit = timeStatistic.roundTimeUnit;
							timeStatistic.runsSinceCurrentBest = 0;
						}
					} else {
						timeStatistic.newBestRun = false;
					}
				}
				
			}
			

			currentSolution.clear();
			if (LOG) System.out.println(timeStatistic);
		} else {
			timeStatistic.newRun = false;
		}
		
		// double result = 0.0;

		double result = Calculations.evaluate(me, others, stations, time, station, timeStatistic);
	
		
		
		if (timeStatistic.newRun) {
			timeStatistic.roundTimeUnit = 0;
		}
		
		timeStatistic.lastValue = time;
		timeStatistic.lastRunCompleted = possibleLastRun(me, new ArrayList<>(others.keySet()), stations);
		if (timeStatistic.roundTimeUnit < time) timeStatistic.roundTimeUnit = time;
		
		
		if (LOG) System.out.printf("[%d] %s Agent: %s Value %f%n",time, station.name, me.name, result);
		
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
	public static Object communication( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData )
	{
		if (LOG) System.out.printf("[%d] Communication: %s Station: %s Arrival: %d Value: %f%n", time, me.name,
				((Station) defaultData[0]).name, (Long) defaultData[1],  (double) defaultData[2]);
		if (round_time_unit < time) round_time_unit = time;
		
		currentSolution.addPrediction(time, me, (Station) defaultData[0],  (Long) defaultData[1], (double) defaultData[2]);
		Calculations.communication(me, others, stations, time, defaultData, timeStatistic);
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
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
		if (round_time_unit < time) round_time_unit = time;
		if (LOG) System.out.printf("[%d] Reward: %s Previous Target: %s Value: %f%n",time,  me.name, me.previousTarget.name, value);
		currentSolution.addReward(time, me);

		Calculations.reward(me, others, stations, time, value, timeStatistic);
	}

	
	
	
	
	private static boolean possibleLastRun(Agent me, List<Agent> others, List<Station> stations) {
		boolean tmp = false;
		for (Station station : stations) {
			if (station.frequency >= 2) return false;
			if (station.frequency == 1) {
				if (tmp) return false;
				tmp = true;
			}
		}
		
		others.add(me);
		for (Agent agent : others) {
			if (agent.frequency > 1) return false;
			for (Map.Entry<Station, Integer> entry : agent.necessities.entrySet()) {
				if (entry.getValue() > 1) return false;
			}
			
			
		}
		return true;
	}
	
}




