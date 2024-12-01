package utilities;

public interface Validator<T> {
    T parse(String input);

    boolean isValid(T value);
}