import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConsumerNode implements Node{
	private OwnConsumer consumer;
	private String title;
	
	public ConsumerNode(OwnConsumer consumer, String title) {
		this.consumer = consumer;
		this.title = title;
	}

	@Override
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		return consumer.compute(me, others, station);
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
	public ConsumerNode copy() {
		return new ConsumerNode(consumer, title);
	}

	
	@Override
	public String toString() {
		return title;
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
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof ConsumerNode consumerNode)) return false;
		return toString().equals(consumerNode.toString());
	}
}
