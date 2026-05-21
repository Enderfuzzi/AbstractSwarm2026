import java.util.function.Predicate;

public class Node<T> {

    private String name;
    private Predicate<T> condition;
    private Node<T> left;
    private Node<T> right;

    private double result;

    private Node(String name) {
        this.name = name;
    }

    public Node(String name, Predicate<T> condition) {
        this(name);
        this.condition = condition;
    }

    public Node(String name, double result) {
        this(name);
        this.result = result;
    }

    public double evaluate(T inputData) {
        if (condition == null) return result;
        return !condition.test(inputData) ? left.evaluate(inputData) : right.evaluate(inputData);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Node<T> getRight() {
        return right;
    }

    public void setRight(Node<T> right) {
        this.right = right;
    }

    public Node<T> getLeft() {
        return left;
    }

    public void setLeft(Node<T> left) {
        this.left = left;
    }
}
