import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OperatorNode implements Node {
	private Operator op;
	private Node left;
	private Node right;
	
	public OperatorNode(Operator op, Node left, Node right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}

	@Override
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		return op.evaluate(left.evaluate(me, others, station), right.evaluate(me, others, station));
	}

	@Override
	public int depth() {
		return Math.max(left.depth(), right.depth()) + 1;
	}
	
	@Override
	public boolean isLeaf() {
		return false;
	}
	
	public void addNode(Node node) {
		if (left.isLeaf()) left = new OperatorNode(Operator.ADDITION, left, node);
		else if (right.isLeaf()) right = new OperatorNode(Operator.ADDITION, right, node);
		else if (left.depth() <= right.depth()) left.addNode(node);
		else right.addNode(node);
	}
	
	@Override
	public String toString() {
		return String.format("(%s %s %s)", left.toString(), op.toString(), right.toString());
	}

	@Override
	public OperatorNode copy() {
		return new OperatorNode(op, left.copy(), right.copy());
	}

	@Override
	public List<Node> getLeafNodes() {
		List<Node> result = new ArrayList<>();
		
		if (left.isLeaf()) result.add(left);
		else result.addAll(left.getLeafNodes());
		
		if (right.isLeaf()) result.add(right);
		else result.addAll(right.getLeafNodes());
		
		return result;
	}

	@Override
	public List<OperatorNode> getOperatorNodes() {
		List<OperatorNode> result = new ArrayList<>();
		
		if (!left.isLeaf()) result.addAll(left.getOperatorNodes());
		result.add(this);
		if (!right.isLeaf()) result.addAll(right.getOperatorNodes());
		
		return result;
	}

	public void setLeft(Node left) {
		this.left = left;
	}
	
	public Node getLeft() {
		return left;
	}
	
	public void setRight(Node right) {
		this.right = right;
	}
	
	public Node getRight() {
		return right;
	}
	
	public void setOperator(Operator op) {
		this.op = op;
	}
	
	public Operator getOperator() {
		return op;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof OperatorNode operatorNode)) return false;
		return op.equals(operatorNode.op) && left.equals(operatorNode.left) && right.equals(operatorNode.right);
	}

	public List<Node> getNodes() {
		ArrayList<Node> result = new ArrayList<>();
		result.addAll(left.getNodes());
		result.add(this);
		result.addAll(right.getNodes());
		return result;
	}
	
}
