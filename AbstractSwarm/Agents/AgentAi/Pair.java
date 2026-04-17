import java.util.Objects;

public record Pair<T, V>(T first, V second) {

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

}
