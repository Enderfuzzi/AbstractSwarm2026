/*
AbstractSwarm agent that always decides for the minimal distance
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	/**
	 * Returns the distance from previous target to target station.
	 *  
	 * @param previousTarget  the previous target station
	 * @param target          the current target station
	 * @return                the distance from previous target to target station, -1 if no path could be found or if previous target or target is null
	 */
	private static int distance( Station previousTarget, Station target )
	{
		// If previousTarget and target exist
	    if( (previousTarget != null) && (target != null) )
	    {
	        // Create open and closed lists
	        Map<StationType, Integer> openList = new HashMap<StationType, Integer>();
	        List<StationType> closedList = new ArrayList<StationType>();
	        Map<StationType, Integer> distances = new HashMap<StationType, Integer>();

	        // Initialize open list with first node
	        openList.put( previousTarget.type, 0 ); 
	        
	        do
	        {
	            // Get and remove node with minimal value
	            int minValue = 0;
	            StationType minStationType = null;
	            for( StationType stationType : openList.keySet() )
	            {
	                if( (minStationType == null) || openList.get( stationType ) < minValue )
	                {
	                    minValue = openList.get( stationType );
	                    minStationType = stationType;
	                }
	            }
	            openList.remove( minStationType );

	            // If goal found, set current distance and stop search
	            if( minStationType == target.type )
	            {
	            	int distanceToTarget = 0;
	                if( distances.containsKey( minStationType ) )
	                {
	                    distanceToTarget = distances.get( minStationType );
	                }
	                else
	                {
	                    distanceToTarget = 0;
	                }

	                return( distanceToTarget );
	            }

	            // If goal not found yet, put to closed list (will no longer be considered)
	            closedList.add( minStationType );

	            // Consider all neighbor nodes
	            for( PlaceEdge placeEdge : minStationType.placeEdges )
	            {
                    StationType neighbor = (StationType) placeEdge.connectedType;

                    // If the current considered node with minimal value is not in the closed list
                    if( !closedList.contains( neighbor ) )
                    {
                        // Calculate actual distance to the neighbor
                        int distance = 0;
                        if( distances.containsKey( minStationType ) )
                        {
                            distance = distances.get( minStationType );
                        }
                        distance += placeEdge.weight;

                        // If node is not in open list or the new distance will be better than the old one
                        if( !openList.containsKey( neighbor ) || (distance < distances.get( neighbor )) )
                        {
                            openList.put( neighbor, distance );  // TODO: Add an estimated value here (without overestimation) for the A* heuristic
                            distances.put( neighbor, distance );
                        }
                    }
	            }
	        }
	        while( !openList.isEmpty() );
	    }

	    // If reached to here no path could be found (or no target or previous target)
	    int distanceToTarget = -1;
	    return( distanceToTarget );
	}
	
	
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
		// Calculate distance
		int distance = distance( me.previousTarget, station );
		
		// If distance is zero, current station is the best choice according to distance
		if( distance == 0 )
		{
			return Double.POSITIVE_INFINITY;
		}
		
		// ...else if no path could be found, current station is the worst choice according to distance
		else if( distance == -1 )
		{
			return Double.NEGATIVE_INFINITY;
		}
		
		// ...else, the higher the distance, the worse
		else
		{
			return 1.0 / distance;
		}
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
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
	}
}
