import java.util.HashMap;

public class MutationStatistic {

	
	public HashMap<Operator, Double> operatorWeight = new HashMap<>();
	
	
	public MutationStatistic() {
		for (Operator op : Operator.values()) {
			operatorWeight.put(op, 0.25);
		}
	}
	
	
	public double get(Operator op) {
		return operatorWeight.getOrDefault(op, 0.25);
	}
	
	public double sum() {
		double result = 0.0;
		for (Double d : operatorWeight.values()) {
			result += d;
		}
		return result;
	}
	
}
