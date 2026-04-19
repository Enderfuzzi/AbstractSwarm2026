import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DataObject {

   enum Attribute {
       TIME,
       AGENT,
       STATION,
       STATIONS,
       PATH_COST;
   }

    private final static String[] COLUM_NAMES = {
            "time",
            "agent_name", "agent_space", "agent_size", "agent_frequency", "agent_necessity",
            "station_name", "station_space", "station_size", "station_frequency", "station_necessity",
            "path_cost"
    };

   private final HashMap<Attribute, Object> values;

   private DataObject(HashMap<Attribute, Object> values) {
       this.values = values;
   }

   public String getHeader() {
       return Arrays.stream(COLUM_NAMES)
               .filter(s -> !s.contains("name"))
               .collect(Collectors.joining(","));
   }

   public double[] getData() {
       double[] result = new double[COLUM_NAMES.length - 2];

       result[0] = ((Long) values.getOrDefault(Attribute.TIME, 0L)).doubleValue();
       if (values.containsKey(Attribute.AGENT)) {
           Agent agent = (Agent) values.get(Attribute.AGENT);
           result[1] = Math.abs(agent.type.size);
           result[2] = agent.type.components.size();
           result[3] = Math.abs(agent.frequency);
           result[4] = Math.abs(agent.type.necessity);
       }

       if (values.containsKey(Attribute.STATION)) {
           Station station = (Station) values.get(Attribute.STATION);
           result[5] = Math.abs(Math.abs(station.space));
           result[6] = station.type.components.size();
           result[7] = Math.abs(station.frequency);
           result[8] = Math.abs(station.type.necessity);
       }

       if (values.containsKey(Attribute.PATH_COST)) {
           result[9] = (Integer) values.get(Attribute.PATH_COST);
       }

       return result;
   }



   @Override
   public String toString() {
       StringBuilder result = new StringBuilder();

       result.append(COLUM_NAMES[0]).append(": ").append(values.getOrDefault(Attribute.TIME, 0L));

       if (values.containsKey(Attribute.AGENT)) {
           Agent agent = (Agent) values.get(Attribute.AGENT);
           result.append(" ").append(COLUM_NAMES[1]).append(": ").append(agent.name);
           result.append(" ").append(COLUM_NAMES[2]).append(": ").append(Math.abs(agent.type.size));
           result.append(" ").append(COLUM_NAMES[3]).append(": ").append(agent.type.components.size());
           result.append(" ").append(COLUM_NAMES[4]).append(": ").append(Math.abs(agent.frequency));
           result.append(" ").append(COLUM_NAMES[5]).append(": ").append(Math.abs(agent.type.necessity));
       }

       if (values.containsKey(Attribute.STATION)) {
           Station station = (Station) values.get(Attribute.STATION);
           result.append(" ").append(COLUM_NAMES[6]).append(": ").append(station.name);
           result.append(" ").append(COLUM_NAMES[7]).append(": ").append(Math.abs(station.space));
           result.append(" ").append(COLUM_NAMES[8]).append(": ").append(station.type.components.size());
           result.append(" ").append(COLUM_NAMES[9]).append(": ").append(Math.abs(station.frequency));
           result.append(" ").append(COLUM_NAMES[10]).append(": ").append(Math.abs(station.type.necessity));
       }

       if (values.containsKey(Attribute.PATH_COST)) {
           result.append(" ").append(COLUM_NAMES[11]).append(": ").append(values.get(Attribute.PATH_COST));
       }


       return result.toString();
   }


   static class DataFactory {

       private final HashMap<Attribute, Object> values = new HashMap<>();

       public DataFactory add(Attribute attribute, Object value) {
           values.put(attribute, value);
           return this;
       }


       public DataObject create() {
           return new DataObject(values);
       }


   }




}


