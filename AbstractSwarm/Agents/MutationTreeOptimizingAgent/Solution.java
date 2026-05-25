import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Solution {

	private HashMap<Agent, Prediction> prediction = new HashMap<>();
	
	private List<Agent> agentsAtStations = new ArrayList<>();
	
	private List<PlanEntry> plan = new ArrayList<>();
	
	
	public void addPrediction(Long time, Agent agent, Station station, Long arrivalTime, double stationValue) {
		//System.out.println("Agent: " + agent.name + " Visting: " + agent.visiting);
		Prediction newPrediction = new Prediction(station, time, arrivalTime , arrivalTime, stationValue);
		if (!prediction.containsKey(agent)) {
			prediction.put(agent, newPrediction);
		} else {
			if (!prediction.get(agent).equals(newPrediction)) {
				prediction.put(agent, newPrediction);
			} else {
				if (!agent.visiting) {
					// consider the earliest decision for a station
					Prediction manipulated = new Prediction(station, prediction.get(agent).predictionTime(),
							prediction.get(agent).predictedArrivalTime() ,arrivalTime, stationValue);
					prediction.put(agent, manipulated);
				}
			}
		}
		
		if (time == prediction.get(agent).arrivalTime()) {
			if (!agentsAtStations.contains(agent)) agentsAtStations.add(agent);
		}
	}
	
	
	public void addReward(Long time, Agent agent) {
		if (agentsAtStations.contains(agent)) {
			Prediction currentPrediction = prediction.remove(agent);
			if (currentPrediction == null) return;
			plan.add(new PlanEntry(agent, currentPrediction.station(), 
					currentPrediction.predictionTime(), currentPrediction.predictedArrivalTime(), 
					currentPrediction.arrivalTime(), time, currentPrediction.stationValue()));
			agentsAtStations.remove(agent);
			Collections.sort(plan);
		}
	}
	
	public List<PlanEntry> getSolution() {
		return plan;
	}
	
	public void clear() {
		prediction.clear();
		agentsAtStations.clear();
		plan.clear();
	}
	

	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		for (PlanEntry entry : plan) {
			result.append(entry);
			result.append("\n");
		}
		return result.toString();
	}
}

record Prediction(Station station, Long predictionTime,  Long predictedArrivalTime, Long arrivalTime, double stationValue) {
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Prediction prediction)) return false;
		if (this.station != prediction.station) return false;
		//if (this.arrivalTime != prediction.arrivalTime) return false;
		if (this.stationValue - prediction.stationValue > 0.0000001) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("[Prediction] Station: %s Prediction Time: %d Arrival time: %d Station value: %f",
				station.name, predictionTime, arrivalTime, stationValue);
	}
};