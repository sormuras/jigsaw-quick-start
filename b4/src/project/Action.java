package project;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

public interface Action {
  Model model();

  default void run(String name, String... args) {
    run(name, List.of(args));
  }

  default void run(String name, Consumer<Arguments<?>> consumer) {
    var arguments = Arguments.of();
    consumer.accept(arguments);
    run(name, arguments.arguments());
  }

  default void run(String name, List<String> arguments) {
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
}
