package project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public record Command(String name, List<String> arguments) implements Runnable {
  public static Command line(String name) {
    return new Command(name, new ArrayList<>());
  }

  Command add(Object argument) {
    arguments.add(argument.toString());
    return this;
  }

  Command add(String key, Object value, Object... more) {
    add(key).add(value);
    if (more.length == 0) return this;
    if (more.length == 1) return add(more[0]);
    if (more.length == 2) return add(more[0]).add(more[1]);
    for (var next : more) add(next);
    return this;
  }

  Command addAll(String... args) {
    for (var arg : args) add(arg);
    return this;
  }

  @Override
  public void run() {
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
