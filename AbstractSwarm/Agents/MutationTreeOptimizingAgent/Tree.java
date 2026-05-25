import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Tree implements Node{
	private Node root;
	
	public Tree(Node root) {
		this.root = root;
	}
	
	public Tree() {
		this(null);
	}

	@Override
	public double evaluate(Agent me, HashMap<Agent, Object> others, Station station) {
		if (isEmpty()) return 0.0;
		return root.evaluate(me, others, station);
	}

	@Override
	public int depth() {
		if (isEmpty()) return -1;
		return root.depth();
	}
	
	@Override
	public boolean isLeaf() {
		if (isEmpty()) return false;
		return root.isLeaf();
	}
	
	@Override
	public void addNode(Node node) {
		if (isEmpty()) {
			root = node;
			return;
		}
		
		root = new OperatorNode(Operator.ADDITION, root, node);
	}

	@Override
	public boolean isEmpty() {
		return root == null;
	}
	
	
	public Tree copy() {
		if (isEmpty()) return new Tree();
		return new Tree(root.copy());
	}
	
	@Override
	public String toString() {
		if (root == null) return "";
		return root.toString();
	}

	@Override
	public List<Node> getLeafNodes() {
		if (isEmpty()) return new ArrayList<>();
		
		return root.getLeafNodes();
	}

	@Override
	public List<OperatorNode> getOperatorNodes() {
		if (isEmpty()) return new ArrayList<>();
		return root.getOperatorNodes();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!(o instanceof Tree tree)) return false;
		if (isEmpty() && tree.isEmpty()) return true;
		if (isEmpty() || tree.isEmpty()) return false;
		return root.equals(tree.root);
	}

	
	public Node getRoot() {
		return root;
	}
	
	public void setRoot(Node node) {
		this.root = node;
	}
	
	public List<Node> getNodes() {
		if (isEmpty()) return new ArrayList<>();
		return root.getNodes();
	}
}
