public record Prediction(
        long time,
        Agent agent,
        Station station,
        double value
) {

    @Override
    public String toString() {
        return String.format("[%d] Agent: %s Station: %s, Value: %f \n", time, agent.name, station.name, value);
    }
}
