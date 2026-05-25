import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ValueNode implements Node {
	private double value;
	
	public ValueNode(double value) {
		this.value = value;
	}

	@Override
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		return value;
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
	
	@Override
	public ValueNode copy() {
		return new ValueNode(value);
	}
	
	@Override
	public String toString() {
		return String.format("%f", value);
	}

	@Override
	public List<Node> getLeafNodes() {
		List<Node> result = new ArrayList<>();
		result.add(this);
		return result;
	}

	@Override
	public List<OperatorNode> getOperatorNodes() {
		return new ArrayList<>();
	}

	
	public void setValue(double value) {
		this.value = value;
	}
	
	public double getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof ValueNode valueNode)) return false;
		return Math.abs(value - valueNode.getValue()) < 0.000001;
	}
	
}
