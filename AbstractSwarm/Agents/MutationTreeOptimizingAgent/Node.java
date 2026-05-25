import java.util.HashMap;
import java.util.List;

public interface Node {
	double evaluate(Agent me, HashMap<Agent, Object> others, Station station);
	
	int depth();
	
	boolean isLeaf();
	
	// no effect on default
	default void addNode(Node node) {};
	
	default boolean isEmpty() {
		return false;
	}
	
	<T extends Node> T copy();
	
	List<Node> getLeafNodes();
	
	List<OperatorNode> getOperatorNodes();
	
	default List<Node> getNodes() {
		return getLeafNodes();
	}
}
