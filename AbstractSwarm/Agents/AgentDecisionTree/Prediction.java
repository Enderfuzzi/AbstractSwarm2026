public record Prediction(
        long time,
        Agent agent,
        Station station,
        double value
) {

    @Override
    public String toString() {
        return String.format("[%d] Prediction | Agent: %s Station: %s, Value: %f", time, agent.name, station.name, value);
    }
}
