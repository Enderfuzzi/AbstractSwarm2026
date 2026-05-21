import java.util.Arrays;
import java.util.HashMap;
import java.util.function.ToDoubleFunction;

public class DataObject {

   enum Attribute {
       TIME(o -> (Double) o),
       AGENT_NAME(o -> -1),
       AGENT_SPACE(o -> (Double) o),
       AGENT_SIZE(o -> (Double) o),
       AGENT_FREQUENCY(o -> (Double) o),
       AGENT_NECESSITY(o -> (Double) o),
       STATION_NAME(o -> -1),
       STATION_SPACE(o -> (Double) o),
       STATION_SIZE(o -> (Double) o),
       STATION_FREQUENCY(o -> (Double) o),
       STATION_NECESSITY(o -> (Double) o),
       // STATIONS,
       PATH_COST(o -> (Double) o),
       MEAN_PATH_COST(o -> (Double) o),

       MAX_AGENT_WORK(o -> (Double) o),
       MAX_STATION_WORK(o -> (Double) o),

       REMAINING_AGENT_WORK(o -> (Double) o),

       REMAINING_STATION_WORK(o -> (Double) o),

       ;

       Attribute() {
           this(o -> Double.valueOf((Integer) o));
       }

       Attribute(ToDoubleFunction<Object> predicate) {
           this.predicate = predicate;
       }
       
       public static final Attribute[] VALUES = Attribute.values();

       public boolean ignored() {
           return switch (this) {
               case AGENT_NAME, STATION_NAME-> true;
               default -> false;
           };
       }

       private final ToDoubleFunction<Object> predicate;

       public double transform(Object o) {
            return predicate.applyAsDouble(o);
       }

   }

   private double[] data;

    /**
     *    TODO add attributes:
     *    How many agents target the station
     *
      */



   private final HashMap<Attribute, Object> values;

   private DataObject(HashMap<Attribute, Object> values) {
       this.values = values;
       transform();
   }

   private void transform() {
       data = new double[getDataSize()];
       int index = 0;
       for (Attribute attribute : Attribute.VALUES) {
           if (attribute.ignored()) {
               index++;
               continue;
           }
           data[attribute.ordinal() - index] =  attribute.transform(values.getOrDefault(attribute, 0));
       }
   }

   public static int getDataSize() {
       return Arrays.stream(Attribute.VALUES).filter(a -> !a.ignored()).toArray(Attribute[]::new).length;
   }


   public double[] getData() {
        return data;
   }


   @Override
   public String toString() {
       StringBuilder result = new StringBuilder();
       int index = 0;
       for (Attribute attribute : Attribute.VALUES) {
           if (attribute.ignored()) {
               index++;
               result.append(" ").append(attribute).append(": ").append(values.getOrDefault(attribute, "None"));
               continue;
           }
           result.append(" ").append(attribute).append(": ").append(data[attribute.ordinal() - index]);
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


