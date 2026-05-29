import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

/**
 * Provides methods to perform tree manipulations.
 *
 * @author Ole Brenner
 * @version 27.05.2026
 */
public class TreeMutation {

	private static final Random random = new Random();

	/**
	 * Makes a copy of the given tree and performs a value mutation of a random node.
	 * The value range is -2.0 to 2.0
	 * @param tree 	The tree to perform the value mutation on.
	 * @return		A copy of the original tree mutated.
	 */
	public static Tree valueMutation(Tree tree) {
		Tree copy = tree.copy();
		if (tree.isEmpty()) return copy;
		
		List<ValueNode> valueNodes = new ArrayList<>();
		for (Node node : copy.getLeafNodes()) {
			if (node instanceof ValueNode valueNode) valueNodes.add(valueNode);
		}
		if (valueNodes.isEmpty()) {
			return consumerWeightMutation(tree); 
		}
		ValueNode randomNode = valueNodes.get(random.nextInt(valueNodes.size()));
		randomNode.setValue(randomNode.getValue() * random.nextDouble(-2.0,2.0));
		
		return copy;
	}


	/**
	 * Makes a copy of the given tree and perform a consumer mutation on the copy.
	 * Nothing happens if there is no consumer node in the tree. Value range is -2.0 to 2.0
	 * @param tree	The tree to perform the consumer mutation on.
	 * @return		A copy of the original tree with mutated consumer
	 */
	public static Tree consumerWeightMutation(Tree tree) {
		Tree copy = tree.copy();
		if (tree.isEmpty()) return copy;
		
		List<ConsumerNode> consumerNodes = new ArrayList<>();
		for (Node node : copy.getLeafNodes()) {
			if (node instanceof ConsumerNode consumerNode) consumerNodes.add(consumerNode);
		}
		if (consumerNodes.isEmpty()) return copy;
		ConsumerNode randomNode = consumerNodes.get(random.nextInt(consumerNodes.size()));
		
		Node tmp = findParentNode(copy, randomNode);
		if (tmp == null) return copy;
		
		Node newNode = new ValueNode(random.nextDouble(-2.0,2.0));
		if (tmp.isLeaf()) {
			copy.setRoot(new OperatorNode(Operator.MULTIPLICATION, tmp, newNode));
			return copy;
		}	
		OperatorNode parent = (OperatorNode) tmp;
		
		if (parent.getLeft().equals(randomNode)) {
			parent.setLeft(new OperatorNode(Operator.MULTIPLICATION, randomNode, newNode));
		}
		
		if (parent.getRight().equals(randomNode)) {
			parent.setRight(new OperatorNode(Operator.MULTIPLICATION, randomNode, newNode));
		}
		
		return copy;
	}
	
	
	
	private static Node findParentNode(Tree tree, Node node) {
		Stack<Node> nodes = new Stack<>();
		if (tree.isEmpty()) return null;
		
		if (tree.getRoot().isLeaf()) {
			if (tree.getRoot().equals(node)) {
				return tree.getRoot();
			}
			return null;
		}
		
		nodes.push(tree.getRoot());
		while (!nodes.isEmpty()) {
			Node current = nodes.pop();
			if (current instanceof OperatorNode operatorNode) {
				if (operatorNode.getLeft().equals(node)) return current;
				if (operatorNode.getRight().equals(node)) return current;
				nodes.push(operatorNode.getLeft());
				nodes.push(operatorNode.getRight());
			}
		}
		return null;
	}

	/**
	 * Performs a crossover between two given trees. Copies both trees.
	 * @param first		The first tree to use
	 * @param second	The second tree
	 * @return			The copied first tree with part of the second tree
	 */
	public static Tree crossover(Tree first, Tree second) {
		Tree firstCopy = first.copy();
		Tree secondCopy = second.copy();
		if (first.isEmpty() && second.isEmpty()) return firstCopy;
		if (first.isEmpty()) return secondCopy;
		if (second.isEmpty()) return firstCopy;
		
		List<Node> firstTreeNodes = firstCopy.getNodes();
		List<Node> secondTreeNodes = secondCopy.getNodes();
		
		if (firstTreeNodes.isEmpty()) return secondCopy;
		if (secondTreeNodes.isEmpty()) return firstCopy;
		
		Node firstRandomNode = firstTreeNodes.get(random.nextInt(firstTreeNodes.size()));
		Node secondRandomNode = secondTreeNodes.get(random.nextInt(secondTreeNodes.size()));
		
		Node parent = findParentNode(firstCopy, firstRandomNode);
		if (firstCopy.getRoot().equals(parent)) {
			firstCopy.setRoot(secondRandomNode);
			return firstCopy;
		}
		if (parent == null) return new Tree(secondRandomNode);
		
		if (parent instanceof OperatorNode operatorNode) {
			if (operatorNode.getLeft().equals(firstRandomNode)) {
				operatorNode.setLeft(secondRandomNode);
				return firstCopy;
			}
			operatorNode.setRight(secondRandomNode);
		}
		return firstCopy;
	}

	/**
	 * Changes a random operator in the given tree. Initial tree is copied.
	 * @param statistic	the mutation statistics for the operators
	 * @param tree		the tree to perform the operator change
	 * @return			the mutated tree
	 */
	public static Tree mutateOperator(MutationStatistic statistic, Tree tree) {
		Tree copy = tree.copy();
		if (copy.isEmpty()) return copy;
		
		List<OperatorNode> nodes = copy.getOperatorNodes();
		if (nodes.isEmpty()) return copy;
		OperatorNode randomNode = nodes.get(random.nextInt(nodes.size()));
		
		double randomValue = random.nextDouble(statistic.sum() - statistic.get(randomNode.getOperator()));
		for (Map.Entry<Operator, Double> entry : statistic.operatorWeight.entrySet()) {
			if (entry.getKey() == randomNode.getOperator()) continue;
			if (randomValue <= entry.getValue()) {
				randomNode.setOperator(entry.getKey());
				break;
			}
			randomValue -= entry.getValue();
		}
		
		return copy;
	}


	/**
	 * Performs a large crossover between two lists of trees
	 * @param first		The first list of trees
	 * @param second	The second list of trees
	 * @return			A copied list consisting of parts of both lists
	 */
	public static List<Tree> largeCrossover(List<Tree> first, List<Tree> second) {
		if (first.isEmpty()) return new ArrayList<>(second);
		if (second.isEmpty()) return new ArrayList<>(first);
		
		int randomIndex = random.nextInt(Math.min(first.size(), second.size()));
		
		List<Tree> firstCopy = new ArrayList<>(first);
		List<Tree> secondCopy = new ArrayList<>(second);
		
		firstCopy.subList(randomIndex, firstCopy.size()).clear();
		secondCopy.subList(0, randomIndex).clear();
		firstCopy.addAll(secondCopy);
		
		return firstCopy;
	}
	
	
}
