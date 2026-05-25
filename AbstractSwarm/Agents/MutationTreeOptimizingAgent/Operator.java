
public enum Operator {
	ADDITION("+"),
	SUBTRACTION("-"),
	MULTIPLICATION("*"),
	DIVISION("/"),
	MODULO("%"),
	
	;
	
	private final String representation;
	
	private Operator(String representation) {
		this.representation = representation;
	}
	
	public double evaluate(double first, double second) {
		if (this == ADDITION) return first + second;
		if (this == SUBTRACTION) return first - second;
		if (this == MULTIPLICATION) return first * second;
		if (this == DIVISION) {
			if (second == 0.0) return first / 1.0;
			return first / second;
		}
		if (this == MODULO) {
			if (second == 0.0) return first % 1;
			return first % second;
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return this.representation;
	}
}