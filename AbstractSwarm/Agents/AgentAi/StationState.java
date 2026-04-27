import org.deeplearning4j.rl4j.space.Encodable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class StationState implements Encodable {

    private final double[] data;

    public StationState(double[] data) {
        this.data = data;
    }

    /**
     * @deprecated
     */
    @Override
    public double[] toArray() {
        return data;
    }

    @Override
    public boolean isSkipped() {
        return false;
    }

    @Override
    public INDArray getData() {
        return Nd4j.create(data);
    }

    @Override
    public Encodable dup() {
        return new StationState(this.data.clone());
    }
}
