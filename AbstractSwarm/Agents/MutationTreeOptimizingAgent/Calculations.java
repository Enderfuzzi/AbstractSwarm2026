import java.util.*;
import java.util.function.Predicate;

public class Calculations {
	
	private static final HashMap<Attribute, Node> attributeNodes = new HashMap<>();
	
	private static HashMap<AgentType, Integer> totalTime = new HashMap<>();
	
	private static boolean firstRun = true;
	
	private static boolean directedTimeEdges = false;
	private static boolean undirectedTimeEdges = false;
	private static boolean stationFrequency = false;
	private static boolean agentFrequency = false;

	private static boolean stationSpace = false;
	private static boolean pathCost = false;
	private static Random random = new Random();

	public static ProbabilityStatistic baseProbability = new ProbabilityStatistic("distribution");
	
	private static MutationStatistic mutationStatistic = new MutationStatistic();

	

	
	private static final boolean TEXT_OUTPUT = false;
	
	private static HashMap<Agent, List<String>> decision = new HashMap<>();
	
	private static List<Tree> currentTrees = new ArrayList<>();
	private static int currentTreeIndex = 0;
	private static List<Tree> bestTrees = new ArrayList<>();
	
	private static boolean generateTree = true;
	
	private static List<Tree> crossoverTree = new ArrayList<>();
	
	
	private static List<FitnessPair> treeFitness = new ArrayList<>();
	
	
	
	private static ProbabilityStatistic mutationProbability = new ProbabilityStatistic();
	
	private static boolean deactivateMutation = false;
	
	private static ProbabilityStatistic basicMutationProbability = new ProbabilityStatistic();


	// keeps track which agent is currently targeting which station
	// the current target is overwritten at each communication call
	private static final HashMap<Agent, Station> VISIT = new HashMap<>();


	public static double evaluate(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station,
			TimeStatistics timeStatistic) {

		
		if (firstRun) {

			PathCalculation.init(stations);
			List<Agent> agents = new ArrayList<>(others.keySet());
			agents.add(me);
			PlacePlanCalculation.init(agents, stations);
			TimePlanCalculation.init(agents, stations);


			directedTimeEdges = graphHasDirectedTimeEdges(stations);
			undirectedTimeEdges = graphHasUndirectedTimeEdges(stations);
			stationFrequency = graphHasStationFrequency(stations);
			agentFrequency = graphHasAgentFrequency(me, others);

			stationSpace = graphHasSpace(stations);
			pathCost = graphHasPathCost(stations);

			attributeNodes.put(Attribute.STATION_FREQUENCY, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(1.0),  
							new ConsumerNode((OwnConsumer) Calculations::stationFrequency, Attribute.STATION_FREQUENCY.name())));
			
			
			attributeNodes.put(Attribute.AGENT_FREQUENCY, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(1.0), 
							new ConsumerNode((OwnConsumer) Calculations::computeAgentFrequency, Attribute.AGENT_FREQUENCY.name())));
			
			attributeNodes.put(Attribute.MAX_DISTRIBUTION, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(1.0),
							new ConsumerNode((OwnConsumer) Calculations::maxDistribution, Attribute.MAX_DISTRIBUTION.name())));
			
			if (directedTimeEdges) {
				attributeNodes.put(Attribute.INCOMING_TIME_CONNECTION, 
						new OperatorNode(Operator.MULTIPLICATION, 
								new ValueNode(1.0), 
								new ConsumerNode((OwnConsumer) Calculations::computeIncomingConnectedStations, Attribute.INCOMING_TIME_CONNECTION.name())));
				
				attributeNodes.put(Attribute.OUTGOING_TIME_CONNECTION, 
						new OperatorNode(Operator.MULTIPLICATION, 
								new ValueNode(1.0), 
								new ConsumerNode((OwnConsumer) Calculations::computeOutgoingConnectedStations, Attribute.OUTGOING_TIME_CONNECTION.name())));
			}
			
			if (undirectedTimeEdges) {
				attributeNodes.put(Attribute.UNDIRECTED_TIME_CONNECTION, 
						new OperatorNode(Operator.MULTIPLICATION,
									new ValueNode(1.0), 
									new ConsumerNode((OwnConsumer) Calculations::computeUndirectedTimeConnectedStations, Attribute.UNDIRECTED_TIME_CONNECTION.name())));
			}
			
			attributeNodes.put(Attribute.PATH_COST, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(-1.0), 
							new ConsumerNode((OwnConsumer) Calculations::pathCost, Attribute.PATH_COST.name())));
			
			attributeNodes.put(Attribute.STATION_SPACE, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(1.0), 
							new OperatorNode(Operator.DIVISION, 
									new ConsumerNode((OwnConsumer) Calculations::stationSpace, "Station Size"), 
									new ConsumerNode((OwnConsumer) Calculations::agentSize, "Agent Size"))));
			
			attributeNodes.put(Attribute.AGENT_TIME, 
					new OperatorNode(Operator.MULTIPLICATION, 
							new ValueNode(1.0), 
							new OperatorNode(Operator.DIVISION, 
									new ConsumerNode((OwnConsumer) Calculations::totalAgentTime, "Total Agent Time"), 
									new ConsumerNode((OwnConsumer) Calculations::estimatedWorkTimeLeft, "Work Time Left"))));


			if (stationSpace) {
				baseProbability.add("space");
			}

			if (pathCost) {
				baseProbability.add("path");
			}
			
			if (directedTimeEdges) {
				baseProbability.add("directedTime");
			}
			
			if (undirectedTimeEdges) {
				baseProbability.add("undirectedTime");
			}
			
			//TODO Trade less mutation rate into more crossover rate

			// mutation 0.6
			// crossover 0.5
			// large crossover 0.3

			// value 0.4
			// operator 0.6

			// TODO more aggressive changes

			mutationProbability.add("mutation", 0.5);
			mutationProbability.add("crossover", 0.4);
			mutationProbability.add("largeCrossover", 0.4);
			
			basicMutationProbability.add("value", 0.4);
			basicMutationProbability.add("operator", 0.6);
		}

		if (!totalTime.containsKey(me.type)) {
			totalTime.put(me.type, estimatedWorkTimeLeft(me, others, station, time));
		}

		double currentFitness = 0.0;
		
		if (timeStatistic.newRun && timeStatistic.lastRunCompleted) {
			if (timeStatistic.newBestRun) {
				
				for (FitnessPair pair : treeFitness) {
					pair.fitness *= 0.9;
				}
			}
			
			if (timeStatistic.currentTwT == 0L) {
				currentFitness = 1.0;
			} else {
				currentFitness = (double) timeStatistic.lowestTwT / timeStatistic.currentTwT;
			}
			
			if (currentFitness > 0.0) {
				treeFitness.add(new FitnessPair(currentFitness, currentTrees));
				
				if (treeFitness.size() > 20) {
					
					FitnessPair toRemove = null;
					for (FitnessPair pair : treeFitness) {
						if (toRemove == null || toRemove.fitness > pair.fitness) {
							toRemove = pair;
						}
					}
					
					if (toRemove != null) treeFitness.remove(toRemove);
				}
			}
			
		}

		
		if (timeStatistic.newBestRun) {
			bestTrees = new ArrayList<>(currentTrees);
		}
		
		
		if (timeStatistic.newRun) {
			currentTrees.clear();
			currentTreeIndex = 0;


			PlacePlanCalculation.reset();
			TimePlanCalculation.reset();
			SpaceCalculation.reset();

			VISIT.clear();
		}
		
		if (timeStatistic.newRun && !generateTree) {
			mutationProbability.newRandom();
			if (mutationProbability.compare("largeCrossover") && !treeFitness.isEmpty()) {
				deactivateMutation = true;
				
				crossoverTree = TreeMutation.largeCrossover(currentTrees, treeFitness.get(random.nextInt(treeFitness.size())).trees);
				
			} else {
				deactivateMutation = false;
			}
		}
		
		Tree evaluation = new Tree();
		
		if (timeStatistic.lastValue != timeStatistic.time) {
			currentTreeIndex++;	
			
			if (generateTree || currentTreeIndex >= bestTrees.size() || (!deactivateMutation && currentTreeIndex >= crossoverTree.size())) {
			
				if (timeStatistic.newRun) {
					if (TEXT_OUTPUT) System.out.println(baseProbability);
					
					baseProbability.newRandom();
					if (timeStatistic.newBestRun || (timeStatistic.lastRunCompleted && timeStatistic.currentTwT <= Math.round(timeStatistic.lowestTwT * 1.6))) {
						baseProbability.reset();
					} else {
						baseProbability.recover();
					}
					
					decision.clear();
					
					if (TEXT_OUTPUT) System.out.println(baseProbability.getAverage());
				}
				
				
				
				
				if (baseProbability.compare("path")) {
					evaluation.addNode(attributeNodes.get(Attribute.PATH_COST));
				}
				
				if (baseProbability.compare("space")) {
					evaluation.addNode(attributeNodes.get(Attribute.STATION_SPACE));
				}
				
				if (baseProbability.compare("distribution")) {
		
					if (stationFrequency) {
						evaluation.addNode(attributeNodes.get(Attribute.STATION_FREQUENCY));
						
					}
					if (agentFrequency) {
						evaluation.addNode(attributeNodes.get(Attribute.AGENT_FREQUENCY));
						
					}
				}
				
				if (baseProbability.compare("directedTime")) {
					evaluation.addNode(attributeNodes.get(Attribute.INCOMING_TIME_CONNECTION));
					evaluation.addNode(attributeNodes.get(Attribute.OUTGOING_TIME_CONNECTION));
				}
				
				if (baseProbability.compare("undirectedTime")) {
					evaluation.addNode(attributeNodes.get(Attribute.UNDIRECTED_TIME_CONNECTION));
				}
				
				
				if ((!stationFrequency && !agentFrequency || !baseProbability.compare("distribution")) && !baseProbability.compare("space") && !baseProbability.compare("path") && 
						!baseProbability.compare("directedTime") && !baseProbability.compare("undirectedTime")) {
					evaluation.addNode(attributeNodes.get(Attribute.MAX_DISTRIBUTION));
				}
				
				if (!decision.containsKey(me)) {
					decision.put(me, baseProbability.getCurrentComparison());
				}
				
			} else {
				
				if (!deactivateMutation) {
					evaluation = bestTrees.get(currentTreeIndex).copy();
	
					if (timeStatistic.newRun) {
						mutationProbability.newRandom();
						basicMutationProbability.newRandom();
					}
					
					if (timeStatistic.lastValue != timeStatistic.time || currentTrees.size() < 1) {
						if (mutationProbability.compare("mutation")) {
							
							if (basicMutationProbability.compare("value")) {
								evaluation = TreeMutation.valueMutation(evaluation);
							} else {
								evaluation = TreeMutation.mutateOperator(mutationStatistic, evaluation);
							}
						}
						
						if (!mutationProbability.compare("mutation") && mutationProbability.compare("crossover") && !treeFitness.isEmpty()) {
							
							List<Tree> crossover = treeFitness.get(random.nextInt(treeFitness.size())).trees;
							
							if (crossover.size() > currentTreeIndex) {
								evaluation = TreeMutation.crossover(evaluation, crossover.get(currentTreeIndex));
							} else {
								evaluation = TreeMutation.crossover(evaluation, crossover.get(crossover.size() - 1));
							}
						}
						
						
						
					} else {
						evaluation = currentTrees.get(currentTrees.size() - 1);
					}
				} else {
					if (crossoverTree.size() > currentTreeIndex) {
						evaluation = crossoverTree.get(currentTreeIndex).copy();
					}
				}
				
			}
			
			
			currentTrees.add(evaluation);
		}
		
		if (!currentTrees.isEmpty()) {
			evaluation = currentTrees.get(currentTrees.size() - 1);
		} 
		
		if (timeStatistic.numberOfRuns >= 30) {
			generateTree = false;
		}
		
		firstRun = false;


		if (TEXT_OUTPUT) System.out.println("-----------------------------");
		if (TEXT_OUTPUT) System.out.println("Undirected result: " + TimePlanCalculation.checkUndirectedEdge(station));
		if (TEXT_OUTPUT) System.out.println("Directed result: " + TimePlanCalculation.checkDirectedEdge(station, others));


		// Do not choose a station which is not reachable
		if (!PathCalculation.reachable(me.previousTarget, station)) {
			return Double.NEGATIVE_INFINITY;
		}

		// the agent size exceeds the station initial space
		if (agentSize(me, others, station, time) > stationSpace(me, others, station, time)) {
			if (TEXT_OUTPUT) System.out.println("Agent to large");
			return Double.NEGATIVE_INFINITY;
		}

		// checks if the agent is allowed to visit a station under space edge and time edge constraints
		if (!PlacePlanCalculation.allowed(me, station)|| !TimePlanCalculation.allowed(station, others)) {
			return Double.NEGATIVE_INFINITY;
		}

		return evaluation.evaluate(me, others, station, time);
	}
	
	public static void communication(Agent me, HashMap<Agent, Object> others, List<Station> stations, 
			long time, Object[] defaultData, TimeStatistics timeStatistic){
		Station target = (Station) defaultData[0];
		long arrival = (Long) defaultData[1];
		double value = (Double) defaultData[2];


		SpaceCalculation.update(me, target, arrival, value);
		VISIT.put(me, target);
	}
	
	public static long lastValue = 0;
	
	public static void reward(Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value,
			TimeStatistics timeStatistic
			) {
		Station visited = VISIT.remove(me);
		if (generateTree || currentTreeIndex >= bestTrees.size()) {
			
			if (decision.containsKey(me)) {
				baseProbability.triggerCompare(decision.get(me), value);
				decision.remove(me);
			}
			
			baseProbability.normalize();
			
			if (lastValue != time) {
				lastValue = time;
				baseProbability.newRandom();
			}

		} else {
			mutationProbability.newRandom();
			basicMutationProbability.newRandom();
		}

		PlacePlanCalculation.addVisit(me, visited);
		TimePlanCalculation.addVisit(me, visited);

		SpaceCalculation.finishedVisit(me);
	}
	
	
	private static double computeAgentFrequency(Agent me,  HashMap<Agent, Object> others, Station station, long time) {
		if (me.frequency == -1) return 0.0;
		double result = 0.0;
		if (agentSize(me, others, station, time) * me.type.components.size() <= stationSpace(station.type)) {
			result += 1.0;
		}
		
		if (TEXT_OUTPUT) System.out.printf("[Agent Frequency]: Station: %s, Agent: %s, Result: %d%n", me.name, station.name, me.type.size);
		// if there are other stations suitable
		List<StationType> used = new ArrayList<>();
		for (VisitEdge edge : me.type.visitEdges) {
			StationType stationType = (StationType) edge.connectedType;
			if (stationType == station.type) continue;
			if (used.contains(stationType)) continue;
			used.add(stationType);
			if (agentSize(me, others, station, time) * me.type.components.size() <= stationSpace(stationType)) {
				result -= 0.5;
			}
			
		}
		
		if (TEXT_OUTPUT) System.out.printf("[Agent Frequency]: Station: %s, Agent: %s, Result: %f%n", me.name, station.name, result);
		
		// if the other agents size exceeds this station priorities it
		List<AgentType> usedAgent = new ArrayList<>();
		usedAgent.add(me.type);
		for (Agent agent : others.keySet()) {
			if (usedAgent.contains(agent.type)) continue;
			usedAgent.add(agent.type);
			
			boolean check = false;
			for (VisitEdge edge: agent.type.visitEdges) {
				StationType stationType = (StationType) edge.connectedType;
				if (stationType == station.type) {
					check = true;
					break;
				}
			}
			if (!check) continue;
			
			if (agentSize(agent, others, station, time) * agent.type.components.size() > stationSpace(station.type)) {
				result += 0.5;
			}
		}
		
		if (station.space >= agentSize(me, others, station, time)) {
			result += 0.5;
		}
		
		if (TEXT_OUTPUT) System.out.printf("[Agent Frequency]: Station: %s, Agent: %s, Result: %f%n", me.name, station.name, result);
		return result;
	}
	
	private static double stationFrequency(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		if (station.frequency == -1) return 0.0;
		double result = 0.0;
		
		if (stationSpace(station.type) != Integer.MAX_VALUE) {
			result = -1.0 * stationTargeted(others, station) + 1.0 * stationSpace(station.type);
		}

		int agentSize = agentSize(me, others,station,time);
		int currentStationSpace = station.space;
		if (currentStationSpace != -1 && agentSize != 0) result += station.space / (double) agentSize(me,others,station, time);
		else if (currentStationSpace != -1) result += currentStationSpace;

		// prefer to stay at a station
		if (me.previousTarget.type == station.type) result += 1.0;

		result -= timeAtStation(me, station.type) * 1.0;
		return result;
	}

	// TODO REPLACE THIS WITH MY PLACE calculation and test

	private static double maxDistribution(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		/*
		if (stationSpace(station.type) != Integer.MAX_VALUE) {
			return -1.0 * stationTargeted(me, others, station) + 1.0 * stationSpace(station.type);
		}
		return (double) 2 / (stationTargeted(me,others,station) + 1);
		 */

		return 1.0 / (SpaceCalculation.nextFreeSlot(me, station, time, PathCalculation.getPathCost(me.previousTarget, station) + time) + 1);
	}

	/**
	 * Extracts the initial space of a station type. Returns 1 if the station type has no space attribute.
	 * @param station The station type to check.
	 * @return The space of a station or 1 if the station has no space attribute
	 */
	private static int stationSpace(StationType station) {
		if (station.space == -1) return Integer.MAX_VALUE;
		return station.space;
	}
	
	/**
	 * Similar to {@link Calculations#stationSpace(StationType)} with unused parameters for {@link OwnConsumer}
	 * @param me unused parameter
	 * @param others unused parameter
	 * @param station The station to check
	 * @return The space of a station or 1 if the station has no space attribute
	 */
	private static int stationSpace(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		int result = stationSpace(station.type);
		return result == Integer.MAX_VALUE ? 100 : result;
	}


	/**
	 * Extracts the size of the given agent. Returns 1 if the agent has no size attribute. Has unused parameters to make it usable with the OwnConsumer interface. 
	 * @param me The agent to check
	 * @param others unused parameter
	 * @param station unused parameter
	 * @return the extracted size of an agent.
	 */
	private static int agentSize(Agent me,  HashMap<Agent, Object> others, Station station, long time) {
		if (me.type.size == -1) return 0;
		return me.type.size;
	}

	public static int stationTargeted(HashMap<Agent, Object> others, Station station) {
		int counter = 0;
		for (Object object : others.values()) {
			if (object == null) continue;
			Object[] communication = (Object[]) object;
			if (((Station) communication[0]) == station) {
				counter += 1;
			}
		}
		if (TEXT_OUTPUT) System.out.println("Station target: " + station.name + " Number: " + counter);
		return counter;
	}

	private static int stationTargeted(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		return stationTargeted(others, station);
	}

	
	record ResultPair(Station station, int cost) {}
	
	private static double computeTimeConnectedStations(Agent me, HashMap<Agent, Object> others, List<ResultPair> connectedStations) {
		double result = 0.0;
		for (ResultPair pair : connectedStations) {
			result += timeAtStation(me, others, pair.station);
			result += pair.cost;
			result += stationTargeted(others, pair.station) * 3;
		}
		return result;
	}
	
	private static List<ResultPair> getTimeConnectedStations(Station station, Predicate<TimeEdge> pred) {
		List<ResultPair> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (pred.test(edge)) continue;
			if (TEXT_OUTPUT) System.out.printf("Time Edge: Station: %s ConnectedType: %s Incoming: %b Outgoing: %b%n", station.name, edge.connectedType.name, edge.incoming, edge.outgoing);
			if (edge.connectedType instanceof StationType stationType) {
				for (Station s : stationType.components) {
					result.add(new ResultPair(s, edge.weight));
				}
			}
		}
		return result;
	}
	
	private static double computeTimeConnectedAgents(Agent me, HashMap<Agent, Object> others, Station station, List<Agent> connectedAgents) {
		double result = 0.0;
		for (Agent agent : connectedAgents) {
			result += estimatedWorkTimeLeft(agent, others, station, 0L);
		}
		return result;
	}
	
	private static List<Agent> getTimeConnectedAgents(Station station, Predicate<TimeEdge> pred) {
		List<Agent> result = new ArrayList<>();
		for (TimeEdge edge : station.type.timeEdges) {
			if (pred.test(edge)) continue;
			if (TEXT_OUTPUT) System.out.printf("Time Edge: Station: %s ConnectedType: %s Incoming: %b Outgoing: %b%n", station.name, edge.connectedType.name, edge.incoming, edge.outgoing);
			if (edge.connectedType instanceof AgentType agentType) {
				result.addAll(agentType.components);
			}
		}
		return result;
	}
	
	// filter undirected, directed outgoing, directed incoming edges 
	private static final Predicate<TimeEdge> undirectedPredicate = edge -> (edge.incoming || edge.outgoing);
	private static final Predicate<TimeEdge> outgoingDirectedPredicate = edge -> (edge.incoming || !edge.outgoing); // !edge.outgoing || edge.incoming);
	private static final Predicate<TimeEdge> incomingDirectedPredicate = edge -> (!edge.incoming || edge.outgoing); //edge.outgoing || !edge.incoming);
	
	
	private static double computeOutgoingConnectedStations(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		double result = 0.0;
		
		result +=  computeTimeConnectedStations(me, others, getTimeConnectedStations(station, outgoingDirectedPredicate));
		
		result +=  computeTimeConnectedAgents(me, others, station, getTimeConnectedAgents(station, outgoingDirectedPredicate));
		
		return result;
	} 
	
	private static double computeIncomingConnectedStations(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		double result = 0.0;
		
		result += computeTimeConnectedStations(me, others, getTimeConnectedStations(station, incomingDirectedPredicate));
		
		result += computeTimeConnectedAgents(me, others, station, getTimeConnectedAgents(station, incomingDirectedPredicate));
		
		return result;
	}
	
	private static double computeUndirectedTimeConnectedStations(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		double result = 0.0;
		
		result += computeTimeConnectedStations(me, others, getTimeConnectedStations(station, undirectedPredicate));
		
		result += computeTimeConnectedAgents(me, others, station, getTimeConnectedAgents(station, undirectedPredicate));
		
		return result;
	}

	
	private static int pathCost(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		// speed is 1 for each scenario
		// if speed should be considered then we have to divide the path cost with the agent speed
		return PathCalculation.getPathCost(me.previousTarget, station);
	}
	
	private static int timeAtStation(Agent me, HashMap<Agent, Object> others, Station station) {
		return timeAtStation(me, station.type);
	}
	
	public static int timeAtStation(Agent me, StationType stationType) {
		if (me.type.time == -1 && stationType.time == -1) return 1;
		if (me.type.time == -1) return stationType.time;
		if (stationType.time == -1) return me.type.time;
		return Math.min(me.type.time, stationType.time);
	}
	
	private static double totalAgentTime(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		return totalTime.get(me.type);
	}
	
	
	private static int estimatedWorkTimeLeft(Agent me, HashMap<Agent, Object> others, Station station, long time) {
		int result = 0;		
		for (Map.Entry<Station, Integer> entry : me.necessities.entrySet()) {
			if (entry.getValue() == 0) continue;
			result += timeAtStation(me, others ,entry.getKey());
		}
		
		if (me.frequency > 0) {
			int lowestTimeAtStation = Integer.MAX_VALUE;
			for (VisitEdge edge : me.type.visitEdges) {
				if (edge.connectedType instanceof StationType stationType) {
					lowestTimeAtStation = Math.min(lowestTimeAtStation, timeAtStation(me, stationType));
				}
			}
			
			result += lowestTimeAtStation * me.frequency;
		}
		
		return result;
	}
	
	// -------------------------------------------------------------------------------------------------
	// collection of methods to check whether the scenario has a given component or not

	/**
	 * Checks if the graph has any time edge matching the predicate.
	 * Note: Connections between two agents are not considered here.
	 * @param stations The stations to check
	 * @param pred The predicate which has to be fulfilled for the edge
	 * @return true if the graph has a matching time edge
	 */
	private static boolean graphHasTimeEdge(List<Station> stations, Predicate<TimeEdge> pred) {
		for (Station station : stations) {
			if (station.type.timeEdges.size() > 0) {
				for (TimeEdge edge : station.type.timeEdges) {
					if (pred.test(edge)) return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks if the graph has any directed time edges.
	 * Uses: {@link Calculations#graphHasTimeEdge(List, Predicate)}
	 * @param stations the stations to check
	 * @return true if the graph has at least one directed time edge
	 */
	private static boolean graphHasDirectedTimeEdges(List<Station> stations) {
		return graphHasTimeEdge(stations, edge -> (edge.incoming || edge.outgoing));
	}
	
	/**
	 * Checks if the graph has any undirected time edges.
	 * Uses: {@link Calculations#graphHasTimeEdge(List, Predicate)}
	 * @param stations the stations to check
	 * @return true if the graph has at least one undirected time edge
	 */
	private static boolean graphHasUndirectedTimeEdges(List<Station> stations) {
		return graphHasTimeEdge(stations, edge -> (!edge.incoming && !edge.outgoing));
	}
	
	/**
	 * Checks if the graph has any stations with frequency attribute.
	 * @param stations The stations to check
	 * @return true if the graph has at least one station with frequency attribute
	 */
	private static boolean graphHasStationFrequency(List<Station> stations) {
		for (Station station : stations) {
			if (station.frequency != -1) return true;
		}
		return false;
	}
	
	/**
	 * Checks if the graph has any agents with frequency attribute.
	 * @param me The current agent
	 * @param others The other agents to check
	 * @return true if the graph has at least one agent with frequency attribute
	 */
	private static boolean graphHasAgentFrequency(Agent me, HashMap<Agent, Object> others) {
		if (me.frequency != -1) return true;
		for (Agent agent : others.keySet()) {
			if (agent.frequency != -1) return true;
		}
		return false;
	}

	/**
	 * Checks if the graph has at least one station with a space attribute.
	 * If there is no station with space attribute then the size of the individual agent is irrelevant for the solution
	 * @param stations all stations in the graph
	 * @return whether there is a station with space attribute
	 */
	private static boolean graphHasSpace(List<Station> stations) {
		for (Station station : stations) {
			if (station.type.space != -1) return true;
		}
		return false;
	}

	/**
	 * Checks if there is any place edge with a weight larger than 0. If not the path calculation it not suitable for generation a solution
	 * @param stations all stations in the graph
	 * @return whether there is an edge with path cost larger than 0
	 */
	private static boolean graphHasPathCost(List<Station> stations) {
		Set<StationType> types = new HashSet<>();
		for (Station station : stations) {
			types.add(station.type);
		}

		for (StationType start : types) {
			for (StationType target : types) {
				if (PathCalculation.getPathCost(start, target) > 0) return true;
			}
		}

		return false;
	}

}

interface OwnConsumer {
	double compute(Agent me, HashMap<Agent, Object> others, Station station, long time);
}


enum Attribute {
	STATION_FREQUENCY,
	AGENT_FREQUENCY,
	OUTGOING_TIME_CONNECTION,
	INCOMING_TIME_CONNECTION,
	UNDIRECTED_TIME_CONNECTION,
	PATH_COST,
	STATION_SPACE,
	AGENT_TIME,
	MAX_DISTRIBUTION
}


class ProbabilityStatistic {
	
	HashMap<String , Pair> map = new HashMap<>();
	
	HashMap<String, Double> initialValues = new HashMap<>();
	
	HashMap<String, List<Double>> pastValues = new HashMap<>();
	
	public ProbabilityStatistic(String...strings) {
		for (String name : strings) {
			this.add(name);
		}
	}
	
	public void add(String name, double threshold) {
		initialValues.put(name, threshold);
		map.put(name, new Pair(threshold));
	}
	
	public void add(String name) {
		add(name, 0.35);
	}
	
	public boolean compare(String name) {
		if (map.containsKey(name)) {
			return map.get(name).compare();
		}
		return false;
	}
	
	public void setThreshold(String name, double newThreshold) {
		if (map.containsKey(name)) {
			map.get(name).threshold = newThreshold;
		}
	}
	
	public void reset() {
		addToPast();
		Pair highest = null;
		for (Pair pair : map.values()) {
			if (highest == null || highest.getThreshold() < pair.getThreshold()) {
				highest = pair;
			}
		}
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			if (entry.getValue() != highest) {
				entry.getValue().setThreshold(computeAverage(entry.getKey()));
			} else {
				entry.getValue().setThreshold(0);
			}
		}
	}
	
	public void recover() {
		for (Map.Entry<String, List<Double>> entry : pastValues.entrySet()) {
			if (map.containsKey(entry.getKey())) {
				if (entry.getValue().size() > 0) {
					map.get(entry.getKey()).setThreshold(entry.getValue().get(entry.getValue().size() - 1));
				} else {
					map.get(entry.getKey()).setThreshold(0.35);
				}
			}
		}
	}
	
	public void newRandom() {
		for (Pair pair : map.values()) {
			pair.newRandom();
		}
	}
	
	public void increaseThreshold(String name, double value) {
		if (map.containsKey(name)) {
			map.get(name).setThreshold(map.get(name).getThreshold() + value);
		}
	}
	
	public void triggerCompare(List<String> parameters, double reward) {
		for (String parameter : parameters) {
			if (map.containsKey(parameter)) {
				map.get(parameter).increaseThreshold(0.2 * reward);
			}
		}
	}
	
	public void normalize() {
		double sum = 0.0;
		for (Pair pair : map.values()) {
			sum += pair.getThreshold();
		}
		if (sum != 0.0) {
			for (Pair pair : map.values()) {
				pair.setThreshold(pair.getThreshold() / sum);
			}
		}
	}
	
	private void addToPast() {
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			if (!pastValues.containsKey(entry.getKey())) {
				pastValues.put(entry.getKey(), new ArrayList<>());
			}
			List<Double> tmpList = pastValues.get(entry.getKey());
			tmpList.add(entry.getValue().getThreshold());
			pastValues.put(entry.getKey(), tmpList);
		}
	}
	
	public double computeAverage(String name) {
		if (pastValues.containsKey(name)) {
			double sum = 0.0;
			for (Double d : pastValues.get(name)) {
				sum += d;
			}
			return sum / (double) pastValues.get(name).size();
			
		}
		return 0.0;
	}
	
	public String getAverage() {
		StringBuilder sb = new StringBuilder();
		for (String name : map.keySet()) {
			sb.append(String.format("[TimeStatistic Average]: %s -> %f\n", name, computeAverage(name)));
		}
		return sb.toString();
	}
	
	public List<String> getCurrentComparison() {
		List<String> result = new ArrayList<>();
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			if (entry.getValue().compare()) {
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	private class Pair {
		private static Random random = new Random();
		
		private double randomValue;
		private double threshold;
		
		public Pair(double threshold) {
			this.threshold = threshold;
			newRandom();
		}
		
		private void checkThreshold() {
			this.threshold = Math.max(this.threshold, 0.05);
		}
		
		public void setThreshold(double value) {
			this.threshold = value;
			checkThreshold();
		}
		
		public void increaseThreshold(double value) {
			this.threshold += value;
			checkThreshold();
		}
		
		public double getThreshold() {
			return this.threshold;
		}
		
		public void newRandom() {
			this.randomValue = random.nextDouble();
		}
		
		public boolean compare() {
			return randomValue <= threshold;
		}
		@Override
		public String toString() {
			return String.format("[Random: %f Threshold %f]", randomValue, threshold);
		}
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Pair> entry : map.entrySet()) {
			sb.append(String.format("[RandomStatistic]: %s -> %s\n", entry.getKey(), entry.getValue().toString()));
		}
		return sb.toString();
	}
	
}


class FitnessPair {
	
	double fitness;
	
	List<Tree> trees;
	
	public FitnessPair(double fitness, List<Tree> trees) {
		this.fitness = fitness;
		this.trees = new ArrayList<>(trees);
	}
	
	public String toString() {
		return String.format("[Fitness=%f,Trees=%s]", fitness, trees.toString());
	}
	
}


