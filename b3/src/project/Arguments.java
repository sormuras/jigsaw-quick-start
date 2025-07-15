package project;

import java.util.List;
import java.util.stream.Stream;

interface Arguments {
  List<String> arguments();

  default void add(Object argument) {
    arguments().add(argument.toString());
  }

  default void add(String key, Object value, Object... more) {
    add(key);
    add(value);
    switch (more.length) {
      case 0 -> {}
      case 1 -> add(more[0]);
      default -> Stream.of(more).forEach(this::add);
    }
  }
}
