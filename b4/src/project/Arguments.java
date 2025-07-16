package project;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface Arguments<T extends Arguments<T>> {
  record ArrayArguments(ArrayList<String> arguments) implements Arguments<ArrayArguments> {}

  static Arguments<?> of(String... arguments) {
    return new ArrayArguments(new ArrayList<>(List.of(arguments)));
  }

  List<String> arguments();

  default T add(Object argument) {
    arguments().add(argument.toString());
    return self();
  }

  default T add(String key, Object value, Object... more) {
    add(key).add(value);
    switch (more.length) {
      case 0 -> {}
      case 1 -> add(more[0]);
      default -> List.of(more).forEach(this::add);
    }
    return self();
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }
}
