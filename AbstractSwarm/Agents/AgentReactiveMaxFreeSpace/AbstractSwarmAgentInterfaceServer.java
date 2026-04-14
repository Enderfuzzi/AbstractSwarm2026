/*
AbstractSwarm - Graphical multi-agent modeling/simulation environment
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


import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class AbstractSwarmAgentInterfaceServer 
{
	public static double evaluation( Agent me, HashMap<Agent, Object> agents, List<Station> stations, long time, Station station )
	{
		return AbstractSwarmAgentInterface.evaluation( me, agents, stations, time, station );
	}
	
	
	public static Object communication( Agent me, HashMap<Agent, Object> agents, List<Station> stations, long time, Object[] defaultData )
	{
		return AbstractSwarmAgentInterface.communication( me, agents, stations, time, defaultData );
	}

	
	public static void reward( Agent me, HashMap<Agent, Object> agents, List<Station> stations, long time, double value )
	{
		AbstractSwarmAgentInterface.reward( me, agents, stations, time, value );
	}

	
	// The Data
	public static Map<String, AbstractSwarmObject> objects = new HashMap<String, AbstractSwarmObject>();
	public static Map<String, AgentType> agentTypes;
	public static Map<String, StationType> stationTypes;
	public static Map<String, VisitEdge> visitEdges;
	public static Map<String, TimeEdge> timeEdges;
	public static Map<String, PlaceEdge> placeEdges;
	public static LinkedHashMap<Agent, Object> agents;
	public static List<Station> stations;
	

	// All commands
	static final String COMMAND_HELLO = "hello";
	static final String COMMAND_PRINT = "print";
	static final String COMMAND_UPDATE = "update";
	static final String COMMAND_EVALUATION = "evaluation";
	static final String COMMAND_COMMUNICATION = "communication";
	static final String COMMAND_REWARD = "reward";
	static final String COMMAND_STOP = "stop";
	static final String[] COMMANDS = new String[]{ COMMAND_HELLO, 
			                                       COMMAND_PRINT, 
			                                       COMMAND_UPDATE, 
			                                       COMMAND_EVALUATION, 
			                                       COMMAND_COMMUNICATION, 
			                                       COMMAND_REWARD, 
			                                       COMMAND_STOP };

	// The communication terminator
	static final String TERMINATOR = "$";

	// The original standard error stream
	static PrintStream originalStdErr = null;
	

	public static void main( String[] args ) 
	{
		// Show start message
		System.out.println( "Java agent interface has started." );
		
		// Redirect standard error to standard out and store original standard out for respond messages
		originalStdErr = System.err;
		System.setErr( System.out );
		
		// Send ready message
		respond( "Java agent interface is ready." );

		boolean isStopped = false;
		while( !isStopped )
		{
			// Listen to the AbstractSwarm stream
			List<String> lines = listen();
			
			
			//
			// Process command
			//
			
			String command = lines.get( 0 ); 
			if( command.contains( COMMAND_HELLO ) )
			{
				respond( "Java is connected." );
			}
			else if( command.contains( COMMAND_PRINT ) )
			{
				for( String line : lines.subList( 1, lines.size() ) )
				{
					System.out.println( line );
				}
				respond( "Text printed." );
			}
			else if( command.contains( COMMAND_STOP ) )
			{
				isStopped = true;
				System.out.println( "Java agent interface will be stopped." );
				respond( "Goodbye!" );
			}
			else if( command.contains( COMMAND_UPDATE ) )
			{
				for( String line : lines.subList( 1, lines.size() ) )
				{
					// DEBUG
					// System.out.println( line );
					
					interpreteJavaCode( line );
				}
				respond( "Components updated." );
			}
			else if( command.contains( COMMAND_EVALUATION ) )
			{
				String line = lines.get( 1 );

				Agent me = (Agent) objects.get( line.split( " " )[ 0 ].trim() );
				LinkedHashMap<Agent, Object> agentsWithoutMe = new LinkedHashMap<Agent, Object>( agents );
				agentsWithoutMe.remove( me );
				List<Station> stationsCopy = new ArrayList<Station>( stations );
				long time = Long.parseLong( line.split( " " )[ 1 ].trim() );
				Station station = (Station) objects.get( line.split( " " )[ 2 ].trim() );

				double result = -999;  // Initialize with a remarkable number which is returned in case of error
				try
				{
					result = evaluation( me, agentsWithoutMe, stationsCopy, time, station );
				}
				catch( Exception e )
				{
					e.printStackTrace();
					
					System.out.println( "\nERROR WHILE EXCECUTING EVALUATION FUNCTION!\n" );
				}
				
				respond( "" + result );
			}
			else if( command.contains( COMMAND_REWARD ) )
			{
				String line = lines.get( 1 );

				Agent me = (Agent) objects.get( line.split( " " )[ 0 ].trim() );
				LinkedHashMap<Agent, Object> agentsWithoutMe = new LinkedHashMap<Agent, Object>( agents );
				agentsWithoutMe.remove( me );
				List<Station> stationsCopy = new ArrayList<Station>( stations );
				long time = Long.parseLong( line.split( " " )[ 1 ].trim() );
				double reward = Double.parseDouble( line.split( " " )[ 2 ].trim() );

				try
				{
					reward( me, agentsWithoutMe, stationsCopy, time, reward );
				}
				catch( Exception e )
				{
					e.printStackTrace();
					
					System.out.println( "\nERROR WHILE EXCECUTING REWARD FUNCTION!\n" );
				}
				
				respond( "Reward processed." );
			}
			else if( command.contains( COMMAND_COMMUNICATION ) )
			{
				String line = lines.get( 1 );

				// If communication should be reset
				if( line.split( " " )[ 1 ].trim().equals( "null" ) )
				{
					Agent me = (Agent) objects.get( line.split( " " )[ 0 ].trim() );
					agents.put( me, null );
					
					respond( "Communication reset." );
				}
				
				// ...else perform call to communication method
				else
				{
					Agent me = (Agent) objects.get( line.split( " " )[ 0 ].trim() );
					LinkedHashMap<Agent, Object> agentsWithoutMe = new LinkedHashMap<Agent, Object>( agents );
					agentsWithoutMe.remove( me );
					List<Station> stationsCopy = new ArrayList<Station>( stations );
					long time = Long.parseLong( line.split( " " )[ 1 ].trim() );
					Station targetStation = (Station) objects.get( line.split( " " )[ 2 ].trim() );
					long timeWhenTargetStationWillBeReached = Long.parseLong( line.split( " " )[ 3 ].trim() );
					double targetEvaluation = Double.parseDouble( line.split( " " )[ 4 ].trim() );
					Object[] defaultData = new Object[]{ targetStation, timeWhenTargetStationWillBeReached, targetEvaluation }; 
	
					try
					{
						Object communicationData = communication( me, agentsWithoutMe, stationsCopy, time, defaultData );
						agents.put( me, communicationData );
					}
					catch( Exception e )
					{
						e.printStackTrace();
						
						System.out.println( "\nERROR WHILE EXCECUTING COMMUNICATION FUNCTION!\n" );
					}
					
					respond( "Communication completed." );
				}
			}
		}
	}
	
	
	private static void idle()
	{
		// Wait a little bit...
		try
		{
			Thread.sleep( 1 );
		} 
		catch( InterruptedException e ) 
		{
			System.out.println( "Error while waiting." );
		}
	}
	
	
	private static List<String> listen()
	{
		List<String> lines = new LinkedList<String>();
		String line = new String();
		while( true )
		{
			try 
			{
				char character = (char) System.in.read();
				if( (character != '\n') && (character != TERMINATOR.charAt( 0 )) )
				{
					line += character;
				}
				else
				{
					lines.add( line );
					
					if( character == TERMINATOR.charAt( 0 ) )
					{
						break;
					}

					line = new String();
				}
			} 
			catch( IOException e ) 
			{
				System.out.println( "Error while reading from standard in." );

				e.printStackTrace();
			}
		}

		return lines;
	}
	
	
	private static void respond( String message )
	{
		System.setErr( originalStdErr );
		System.err.println( message + TERMINATOR );
		System.setErr( System.out );
	}
	
	
	public static void interpreteJavaCode( String line )
	{
		// Blank lines are skipped
		if( !line.equals( "" ) )
		{
			// If not a list operation on the general stations/agents lists
			if( !line.trim().startsWith( "stations" ) && !line.trim().startsWith( "agents" ) )
			{
				//
				// Get the main object name, the argument and the field name to which the argument should be applied...
				//
				
				String objectName = line.split( "\\." )[ 0 ].trim();
				String argument = null;
				String fieldName = null;
				
				// If assignment
				if( line.contains( "=" ) )
				{
					// Case a.b.c = ... ;
					fieldName = line.split( "\\." )[ 1 ].trim();
	
					// Case a.b = ... ;
					if( fieldName.contains( "=" ) )
					{
						fieldName = fieldName.split( "\\=" )[ 0 ].trim();
					}
					
					// For assignments, arguments can be strings, numbers, objects or newly created ArrayLists
					argument = line.split( "\\=" )[ 1 ].split( ";" )[ 0 ].trim();
				}
	
				// ...else if list or hash map operation (add/put)
				else
				{
					// Case a.b.add( ... );
					fieldName = line.split( "\\." )[ 1 ].trim();
					
					// For list operations, arguments are an operation (add) together with the argument in ( ... )
					if( line.contains( "add(" ) )
					{
						argument = line.substring( line.indexOf( "add(" ), line.indexOf( ";" ) ).trim();
					}
					
					// ...else must be a put-operation of a hash map
					else
					{
						argument = line.substring( line.indexOf( "put(" ), line.indexOf( ";" ) ).trim();
					}
				}
								
				
				//
				// Only create new object (key) if it does not yet exist
				//
				
				if( !objects.keySet().contains( objectName ) )
				{
					if( objectName.startsWith( "stationType" ) )
						objects.put( objectName, new StationType() );
					else if( objectName.startsWith( "agentType" ) )
						objects.put( objectName, new AgentType() );
					else if( objectName.startsWith( "station" ) )
						objects.put( objectName, new Station() );
					else if( objectName.startsWith( "agent" ) )
						objects.put( objectName, new Agent() );
					else if( objectName.startsWith( "edge_visitEdge" ) )
						objects.put( objectName, new VisitEdge() );
					else if( objectName.startsWith( "edge_timeEdge" ) )
						objects.put( objectName, new TimeEdge() );
					else if( objectName.startsWith( "edge_placeEdge" ) )
						objects.put( objectName, new PlaceEdge() );
				}
				
				
				//
				// Set the read field or list operation, etc.
				//
				
				Class<? extends AbstractSwarmObject> objectClass = objects.get( objectName ).getClass();
				try 
				{
					Field field = objectClass.getField( fieldName );
					
					// If a string was assigned...
					if( argument.startsWith( "\"" ) )
					{
						field.set( objects.get( objectName ), argument.substring( 1, argument.length() - 1 ) );
					}
					
					// ...else if an new array list was assigned
					else if( argument.startsWith( "new ArrayList" ) )
					{
						// Create new list depending on the element type
						String elementType = argument.split( "\\<" )[ 1 ].split( "\\>" )[ 0 ].trim();
						if( elementType.equals( "Station" ) )
						{
							field.set( objects.get( objectName ), new ArrayList<Station>() );
						}
						else if( elementType.equals( "Agent" ) )
						{
							field.set( objects.get( objectName ), new ArrayList<Agent>() );
						}
						
						// ...in case of edges, theses are currently always added to the component type as sub-object (e. g., case a.type.visitEdges = ...)
						else if( elementType.equals( "VisitEdge" ) )
						{
							ComponentType componentType = (ComponentType) field.get( objects.get( objectName ) ); 
							if( componentType instanceof StationType )
							{
								StationType stationType = (StationType) componentType;
								stationType.visitEdges = new ArrayList<VisitEdge>();
							}
							else
							{
								AgentType agentType = (AgentType) componentType;
								agentType.visitEdges = new ArrayList<VisitEdge>();
							}
						}
						else if( elementType.equals( "PlaceEdge" ) )
						{
							ComponentType componentType = (ComponentType) field.get( objects.get( objectName ) ); 
							if( componentType instanceof StationType )
							{
								StationType stationType = (StationType) componentType;
								stationType.placeEdges = new ArrayList<PlaceEdge>();
							}
							else
							{
								AgentType agentType = (AgentType) componentType;
								agentType.placeEdges = new ArrayList<PlaceEdge>();
							}
						}
						else if( elementType.equals( "TimeEdge" ) )
						{
							ComponentType componentType = (ComponentType) field.get( objects.get( objectName ) ); 
							if( componentType instanceof StationType )
							{
								StationType stationType = (StationType) componentType;
								stationType.timeEdges = new ArrayList<TimeEdge>();
							}
							else
							{
								AgentType agentType = (AgentType) componentType;
								agentType.timeEdges = new ArrayList<TimeEdge>();
							}
						}
						// TODO ...add further lists here if needed
					}

					// ...else if an new hash map was assigned
					else if( argument.startsWith( "new HashMap" ) )
					{
						// Create new hash map depending on the element type
						String elementType = argument.split( "\\<" )[ 1 ].split( "," )[ 0 ].trim();
						if( elementType.equals( "Station" ) )
						{
							field.set( objects.get( objectName ), new HashMap<Station, Integer>() );
						}
						else if( elementType.equals( "Agent" ) )
						{
							field.set( objects.get( objectName ), new HashMap<Agent, Integer>() );
						}
						else if( elementType.equals( "TimeEdge" ) )
						{
							field.set( objects.get( objectName ), new HashMap<TimeEdge, Integer>() );
						}
						// TODO ...add further hash maps here if needed
					}
					
					// ...else if it was a list operation (add)
					else if( argument.startsWith( "add(" ) )
					{
						// Get the owner of the container, i.e., the object to which the list belongs (depending on whether it is encapsulated in another object or the main object itself):
						// List operations are always done on lists as sub-objects, e. g., a.type.components.add( ... ), a.visitEdges.add( ... ), etc.
						AbstractSwarmObject containerOwner = null; 
						if( objectName.startsWith( "agent_" ) || objectName.startsWith( "station_" ) )
						{
							containerOwner = (AbstractSwarmObject) field.get( objects.get( objectName ) ); 
						}
						else
						{
							containerOwner = objects.get( objectName );
						}
						
						// Get the container to which the argument will be added
						String beforeAdd = line.split( "\\.add\\(" )[ 0 ];
						String containerName = beforeAdd.split( "\\." )[ beforeAdd.split( "\\." ).length - 1 ]; 
						@SuppressWarnings("unchecked")
						List<AbstractSwarmObject> container = (List<AbstractSwarmObject>) containerOwner.getClass().getField( containerName ).get( containerOwner );
						
						// Add object to the retrieved container
						String argumentName = argument.split( "add\\(" )[ 1 ].split( "\\)" )[ 0 ].trim();
						AbstractSwarmObject argumentObject = objects.get( argumentName );
						container.add( argumentObject );
						
						// TODO If an edge was added, maybe delete it here from "objects", since it is correctly embedded now in the respective object's container?  
					}

					// ...else if it was a hash map operation (put)
					else if( argument.startsWith( "put(" ) )
					{
						// Get the owner of the container, i.e., the object to which the list belongs
						AbstractSwarmObject containerOwner = objects.get( objectName );
						
						// Get the container to which the argument will be added
						String beforePut = line.split( "\\.put\\(" )[ 0 ];
						String containerName = beforePut.split( "\\." )[ beforePut.split( "\\." ).length - 1 ]; 
						@SuppressWarnings("unchecked")
						HashMap<AbstractSwarmObject, Integer> container = (HashMap<AbstractSwarmObject, Integer>) containerOwner.getClass().getField( containerName ).get( containerOwner );
						
						
						//
						// Add the key-value pair to the retrieved container
						//
						
						String argumentNameKey = argument.split( "put\\(" )[ 1 ].split( "," )[ 0 ].trim();
						String argumentNameValue = argument.split( "put\\(" )[ 1 ].split( "," )[ 1 ].split( "\\)" )[ 0 ].trim();
						
						// If object to put does not yet exist, create it first (will be filled later)!
						if( !objects.keySet().contains( argumentNameKey ) )
						{
							if( argumentNameKey.startsWith( "agent_" ) )
							{
								objects.put( argumentNameKey, new Agent() );
							}
							else
							{
								objects.put( argumentNameKey, new Station() );
							}
							// FOR OTHER OBJECTS (e.g. TIME EDGES) THIS WILL NOT BE NEEDED, SINCE THE STATIC GRAPH ENVIRONMENT IS
							// ALWAYS TRANSFERRED FIRST BEFORE THE DYNAMIC PART LIKE NECESSITIES OR CYCLES IS TRANSFERRED; FOR AGENTS
							// AND STATIONS THIS IS A SPECIAL CASE SINCE, E.G., FOR NECESSITIES IT IS REQUIRED THAT THE CONNECTED COMPONENT(S)
							// ALREADY EXIST - WHICH MAY NOT BE THE CASE DUE TO THE CREATION ORDER! 
						}
						
						AbstractSwarmObject argumentObject = objects.get( argumentNameKey );
						int argumentInteger = Integer.parseInt( argumentNameValue );
						container.put( argumentObject, argumentInteger );
					}
					
					// ...else numeric or boolean or object assignment (including null)
					else
					{
						// Try if the value assigned is numeric...
						try
						{
							int valueAsInt = Integer.parseInt( argument );
							field.set( objects.get( objectName ), valueAsInt );
						}
						
						// ...otherwise must be boolean, null or an object assignment
						catch( NumberFormatException e )
						{
							// Check for boolean
							if( argument.equals( "true" ) || argument.equals( "false" ) )
							{
								boolean valueAsBoolean = Boolean.parseBoolean( argument );
								field.set( objects.get( objectName ), valueAsBoolean );
							}
							
							// ...else if null assignment...
							else if( argument.equals( "null" ) )
							{
								field.set( objects.get( objectName ), null );
							}
							
							// ...else must be object assignment
							else
							{
								// If the argument type is not specific, infer specific type
								if( argument.startsWith( "componentType_" ) )
								{
									String agentTypeArgument = argument.replace( "componentType_", "agentType_" );
									String stationTypeArgument = argument.replace( "componentType_", "stationType_" );
									if( objects.keySet().contains( agentTypeArgument ) )
									{
										argument = agentTypeArgument;
									}
									else if( objects.keySet().contains( stationTypeArgument ) )
									{
										argument = stationTypeArgument;
									}
								}
								// TODO ADD FURTHER INFERENCE FOR OTHER TYPES HERE
								// ...
								
								field.set( objects.get( objectName ), objects.get( argument ) );
							}
						}
					}
				} 
				catch( NoSuchFieldException e ) 
				{
					System.out.println( "Error: Field does not exist." );
				} 
				catch( SecurityException e ) 
				{
					System.out.println( "Error: Security exception while setting a field." );
				} 
				catch( IllegalArgumentException e )  
				{
					System.out.println( "Error: Illegal argument while setting a field." );
				} 
				catch( IllegalAccessException e ) 
				{
					System.out.println( "Error: Illegal access while setting a field." );
				}
			}
			
			// ...else (if list operation on the general stations/agents lists)
			else
			{
				// If list/hash map is newly created
				if( line.contains( "new ArrayList" ) || line.contains( "new LinkedHashMap" ) )
				{
					if( line.trim().startsWith( "stations" ) )
					{
						stations = new ArrayList<Station>();
					}
					else
					{
						agents = new LinkedHashMap<Agent, Object>();
					}
				}
				
				// ...else (must be adding/putting)
				else
				{
					if( line.trim().startsWith( "stations" ) )
					{
						// Get argument
						String addArgument = line.split( "add\\(" )[ 1 ].split( "\\)" )[ 0 ].trim();

						Station station = (Station) objects.get( addArgument ); 
						stations.add( station ); 
					}
					else
					{
						// Get arguments
						String addArgument1 = line.split( "put\\(" )[ 1 ].split( "\\," )[ 0 ].trim();
						String addArgument2 = line.split( "put\\(" )[ 1 ].split( "\\," )[ 1 ].split( "\\)" )[ 0 ].trim();
						
						Agent agent = (Agent) objects.get( addArgument1 );
						if( addArgument2.equals( "null" ) )
						{
							// This case should not happen, but due to a bug in the transfer code, it will be simply ignored here
							if( agents != null )
							{
								agents.put( agent, null );
							}
						}
						else
						{
							agents.put( agent, null /* TODO Put anything else here? */ );
						}
					}
				}
			}
		}
	}
}





class AbstractSwarmObject
{
}





class ComponentType extends AbstractSwarmObject
{
	public String name;
	public String type;

	public int frequency;
	public int necessity;
	public int time;
	public int cycle;
	
	public List<VisitEdge> visitEdges;
	public List<TimeEdge> timeEdges;
	public List<PlaceEdge> placeEdges;  /* TODO Not needed in AgentType, but for backward compatibility reasons... */
}


class StationType extends ComponentType
{
	public List<Station> components;
	
	public int space;
}


class AgentType extends ComponentType
{
	public List<Agent> components;

	public int size;
	public int priority;
}





class Edge extends AbstractSwarmObject
{
	public ComponentType connectedType;
	public String type;
}


class VisitEdge extends Edge
{
	public boolean bold;
}


class WeightedEdge extends Edge
{
	public boolean incoming;
	public boolean outgoing;
	public int weight;
}


class PlaceEdge extends WeightedEdge
{
}


class TimeEdge extends WeightedEdge
{
	public boolean andOrigin;
	public boolean andConnected;
}





class Component extends AbstractSwarmObject
{
	public String name;
	
	public int frequency;
	public Map<TimeEdge, Integer> cycles;
}


class Station extends Component
{
	public StationType type;

	public int space;
	public Map<Agent, Integer> necessities;
}


class Agent extends Component
{
	public AgentType type;
	
	public int time;
	public Map<Station, Integer> necessities;
	public Station target;
	public Station previousTarget;
	public boolean visiting;
}