import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class Network {

    private MultiLayerNetwork model;

    private final int inputSize;

    private static final double LR = 0.001;

    private final HashMap<Agent, Pair> predictions = new HashMap<>();

    private final HashMap<AgentType, Double> mean = new HashMap<>();
    private final HashMap<AgentType, Integer> meanCount = new HashMap<>();

    public Network(int inputSize) {
        this.inputSize = inputSize;
        init();
    }

    private record Pair(double stationValue, double[] data) {

    }


    private void init() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(123L)
                .weightInit(WeightInit.RELU)
                .updater(new Adam(LR))
                .list()
                .layer(new DenseLayer.Builder().nIn(inputSize).nOut(32)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder().nIn(32).nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder().nIn(64).nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder().nIn(128).nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder().nIn(64).nOut(32)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1)
                        .activation(Activation.SOFTPLUS) // Erlaubt freie Skalierung der Utility
                        .build())
                .build();

        model = new MultiLayerNetwork(config);
        model.init();
    }

    public void save() {
        File file = new File("Model.zip");
        try {
            ModelSerializer.writeModel(model, file, true);
        } catch (IOException e) {
            // throw new RuntimeException(e);
        }
    }

    public void load() {
        File file = new File("Model.zip");
        try {
            model = ModelSerializer.restoreMultiLayerNetwork(file);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }

    public void resetEpsilon() {
        epsilon = EPSILON_START;
    }

    private INDArray transform(double[] inputData) {
        return Nd4j.create(inputData).reshape(1, inputSize);
    }

    private double epsilon = EPSILON_START;
    private final static double EPSILON_START = 0.95;
    private final static double EPSILON_DECAY = 1.0;
    private static final Random RANDOM = new Random();


    public double predict(Agent agent, double[] inputData, boolean isAllowed) {
        INDArray input = transform(inputData);
        double prediction = model.output(input).getDouble(0);

        prediction += (RANDOM.nextDouble() * 2 - 1) * 0.01;


        // never choose a station which is not allowed
        if (!isAllowed) prediction = -1000;


        if (predictions.containsKey(agent)) {
            if (prediction > predictions.get(agent).stationValue)
                predictions.put(agent, new Pair(prediction, inputData));
        } else {
            predictions.put(agent, new Pair(prediction, inputData));
        }

        // double currentMaxPrediction = predictions.getOrDefault(agent, new Pair(-100.0, inputData)).stationValue;
        if (RANDOM.nextDouble() > epsilon) {
            // replace the prediction with the actual value
            predictions.put(agent, new Pair(prediction, inputData));
            // never choose a station which is not allowed
            return isAllowed ? 100 : -1000;
        }

        return prediction;
    }


    private static final double TRAIN_BASELINE = 0.5;
    private static final double TRAIN_RATE = 1.0;

    private static final Random random = new Random();



    public void train(Agent agent, double reward, long bestTime, long time) {
        if (!predictions.containsKey(agent)) return;
        INDArray input = transform(predictions.get(agent).data);

        double stationValue = predictions.get(agent).stationValue;
        AgentType agentType = agent.type;

        double rewardEfficiency = (double) bestTime / time;
        reward *= Math.pow(rewardEfficiency, 2);


        if (!mean.containsKey(agentType)) {
            mean.put(agentType, reward);
            meanCount.put(agentType, 1);
        }

        double weightedReward = Math.pow(reward, 2);

        System.out.printf("Scaled reward: %f%n", weightedReward);

        double change = stationValue + (weightedReward * TRAIN_RATE);
        // double change = (mean.get(agentType) - reward) * TRAIN_RATE * stationValue * 0.1;
        // reduce target value if not better than average
        // reduce target value if equal to test other stations
        // double targetValue = mean.get(agentType) >= reward ? stationValue - change : stationValue + change;

        double targetValue;
        if (mean.get(agentType) < reward) {
            targetValue = stationValue + weightedReward * TRAIN_RATE;
        } else {
            targetValue = stationValue - (mean.get(agentType) - reward + 0.05) * TRAIN_RATE;
        }


        meanCount.put(agentType, meanCount.get(agentType) + 1);
        double newAverage = mean.get(agentType) + (reward - mean.get(agentType)) / meanCount.get(agentType);
        mean.put(agentType, newAverage);

        epsilon = Math.max(0.01, epsilon * EPSILON_DECAY);

        model.fit(input, Nd4j.scalar(targetValue).reshape(1, 1));
    }
}
