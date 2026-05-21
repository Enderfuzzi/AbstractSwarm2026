import java.util.HashMap;
import java.util.function.Predicate;

public class Nodes {

    enum KEY {
        IS_ALLOWED,
        SLOT_PLANNING,
        TIME
        ;
    }


    private static final HashMap<KEY, Predicate<InputData>> NODES = new HashMap<>();


    public static void init() {
        NODES.put(KEY.IS_ALLOWED, data -> {
            if (Utils.getSize(data.agent()) > Utils.getInitSpace(data.station())) return false;
            if (!PlacePlanCalculation.allowed(data.agent(), data.station())) return false;
            if (!TimePlanCalculation.allowed(data.station())) return false;

            return true;
        });

    }


    public static Node<InputData> generateNode(KEY key, Node<InputData> notFulfilled, Node<InputData> fulfilled) {
        Node<InputData> node =  new Node<>(key.name(), NODES.getOrDefault(key, data -> false));
        node.setLeft(notFulfilled);
        node.setRight(fulfilled);
        return node;
    }

    public static Node<InputData> generateAllowedNode() {
        return generateNode(KEY.IS_ALLOWED, new Node<>("Leaf", Double.NEGATIVE_INFINITY), null);
    }



}
