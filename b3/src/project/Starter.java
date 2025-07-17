package project;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface Starter extends Action {
  record Java(String name, List<String> arguments) implements Command.Context<Java> {
    private Java() {
      this("java", new ArrayList<>());
    }
  }

  default void start(String... args) {
    var java = new Java();
    startWithModulePath(java);
    startWithModule(java);
    startWithArguments(java, args);
    java.run();
  }

  default void startWithModulePath(Java java) {
    java.add("--module-path", model().folders().out().resolve("modules"));
  }

  default void startWithModule(Java java) {
    java.add("--module", "com.greetings");
  }

  default void startWithArguments(Java java, String... args) {
    java.addAll(Stream.of(args));
  }
}
