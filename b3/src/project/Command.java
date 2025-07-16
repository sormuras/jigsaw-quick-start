package project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/// A command-line wrapping API.
///
/// For example, the following shell command-line:
/// ```shell
/// java --show-version --describe-module java.base
/// ```
/// can be composed and run via:
/// {@snippet lang = java:
/// Command.line("java")
///     .add("--show-version")
///     .add("--describe-module", "java.base")
///     .run();
/// }
public interface Command extends Runnable {
  String name();

  List<String> arguments();

  @Override
  default void run() {
    var name = name();
    var arguments = List.copyOf(arguments());
    System.out.println("| " + name + " " + String.join(" ", arguments));
    var tool = ToolProvider.findFirst(name);
    if (tool.isPresent()) {
      var args = arguments.toArray(String[]::new);
      var code = tool.get().run(System.out, System.err, args);
      if (code == 0) return;
      throw new RuntimeException(name + " returned non-zero exit code: " + code);
    }
    var program = Path.of(System.getProperty("java.home"), "bin", name);
    var builder = new ProcessBuilder(program.toString());
    try {
      builder.command().addAll(arguments);
      var process = builder.inheritIO().start();
      var code = process.waitFor();
      if (code == 0) return;
      throw new RuntimeException(name + " returned non-zero exit code: " + code);
    } catch (Exception exception) {
      throw new RuntimeException(name + " failed.", exception);
    }
  }

  static Line line(String name, Object... arguments) {
    return new Line(name, new ArrayList<>()).addAll(Stream.of(arguments));
  }

  record Line(String name, List<String> arguments) implements Context<Line> {}

  interface Context<T extends Context<T>> extends Command {
    default T add(Object argument) {
      arguments().add(argument.toString());
      return self();
    }

    default T add(String key, Object value, Object... more) {
      add(key).add(value);
      switch (more.length) {
        case 0 -> {}
        case 1 -> add(more[0]);
        case 2 -> add(more[0]).add(more[1]);
        default -> addAll(Stream.of(more));
      }
      return self();
    }

    default T addAll(Stream<?> arguments) {
      arguments().addAll(arguments.map(Object::toString).toList());
      return self();
    }

    @SuppressWarnings("unchecked")
    private T self() {
      return (T) this;
    }
  }
}
