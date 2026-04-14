/*
AbstractSwarm Agent that considers directed place edges to some degree
Copyright (C) 2020  Ideas emerged from joint efforts by Daan Apeldoorn
                    (daan.apeldoorn@uni-mainz.de), Lars Hadidi
					(lahadidi@uni-mainz.de), Torsten Panholzer and 
					Dativa Tibyampansha.

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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


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
 * <br />
 * <br />
 * 
 * 
 * The agent implementation provided here allows reactive free space maximizing agents to also consider directed place edges to some degree (as primary criterion). 
 * It involves ideas that emerged from joint efforts by Daan Apeldoorn, Lars Hadidi, Torsten Panholzer and Dativa Tibyampansha.
 */
public class AbstractSwarmAgentInterface 
{
	/** The agent's random generator. */
	static Random random = new Random();
	
	
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
	public static double evaluation( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		// Check whether there are any incoming place edges at the connected types
		boolean hasIncomingDirPlaceEdge = false;
		for( VisitEdge visitEdge : me.type.visitEdges )
		{
			for( PlaceEdge placeEdge : ((StationType) visitEdge.connectedType).placeEdges )
			{
				if( placeEdge.incoming )
				{
					hasIncomingDirPlaceEdge = true;
					break;
				}
			}
		}

		// If one of the connected stations has an incoming directed place edge
		if( hasIncomingDirPlaceEdge )
		{
			// Choose the one with the fewest transitive contraints
			return -inPlaceEdgeLinearTransClosure( me, station.type ).size();
		}
		
		// ...else choose the one with the maximum free space
		{
			return evaluationMaxFreeSpace( me, others, stations, time, station );
		}
	}
	

	/**
	 * This method is borrowed from maximum free space agents.
	 * 
	 * @see AgentReactiveMaxFreeSpace
	 */
	public static double evaluationMaxFreeSpace( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		// Initialize the free space with the current station's remaining space
		double freeSpace = station.space;

		// If station not limited in space, free space will be infinite
		if(	station.space == -1 )
		{
			return Double.POSITIVE_INFINITY;
		}
		
		// Iterate over all other agents
		for( Agent other : others.keySet() )
		{
			// If other agent has communicated the current station as target
			if( (others.get( other ) != null) && ((Object[]) others.get( other ))[ 0 ] == station )
			{
				// If other agent has a limited size, subtract it from free space
				if( other.type.size != -1 )
				{
					freeSpace -= other.type.size;
				}
				
				// ...else remaining free space will be completely covered
				else if( freeSpace > 0 )
				{
					freeSpace = 0;
				}
			}
		}
		
		return freeSpace;
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
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
	}
	
	
	/**
	 * Returns the linear transitive close (randomly branched) of the incoming place edge relation for a provided station type regarding the station types that are connected to the provided agent's type.
	 * The resulting list contains the first-order connected station type as last element, the second-order connected station type as second to last element, and so on.
	 * 
	 * @param me           the agent for whose types that are connected by visit edges the closure will be calculated (station types not connected to the agent's type are not relevant since they cannot be reached)    
	 * @param stationType  the station type for which the linear transitive close (randomly branched) of the incoming place edge relation will be calculated
	 * @return             the linear transitive close (randomly branched) of the incoming place edge relation
	 */
	private static List<StationType> inPlaceEdgeLinearTransClosure( Agent me, StationType stationType )
	{
		// Init list containing the in place edge linear transitive closure;
		// set the next station type to be investigated to the current one 
		List<StationType> result = new ArrayList<StationType>();
		StationType nextStationType = stationType;
		
		// Create linear transitive closure (branch randomly in case multiple incoming place edges);
		// stop if there are no more incoming place edges
		do
		{
			// Get all station types connected via incoming place edges
			List<StationType> inConnectedStationTypes = new ArrayList<StationType>();
			for( PlaceEdge placeEdge : nextStationType.placeEdges )
			{
				if( placeEdge.incoming )
				{
					// Check if connected station type is also connected to the agent's type by a visit edge (otherwise the station type is not relevant here and should not be considered)
					boolean isConnectedToAgent = false;
					for( VisitEdge visitEdge : me.type.visitEdges )
					{
						if( visitEdge.connectedType == placeEdge.connectedType )
						{
							isConnectedToAgent = true;
							break;
						}
					}
					
					if( isConnectedToAgent )
					{
						inConnectedStationTypes.add( (StationType) placeEdge.connectedType );
					}
				}
			}
			
			// If list is not empty, choose one of the connected station types randomly
			nextStationType = null;
			if( !inConnectedStationTypes.isEmpty() )
			{
				nextStationType = inConnectedStationTypes.get( random.nextInt( inConnectedStationTypes.size() ) );
				
				// Check for cycles:
				// If the next station type has not been visited already...
				if( !result.contains( nextStationType ) )
				{
					result.add( nextStationType );
				}
				
				// ...else (if the next station type has been visited already)
				else
				{
					break; 
				}
			}
		}
		while( nextStationType != null );

		return result;
	}
}
