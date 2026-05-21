import java.util.HashMap;
import java.util.List;

public record InputData(
        Agent agent,
        HashMap<Agent, Object> others,
        List<Station> stations,
        long time,
        Station station
) {


}