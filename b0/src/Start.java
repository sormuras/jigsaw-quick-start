import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public class Start {
  public static void main(String... args) {
    var out = Path.of("b0", "out");
    if (!Files.isDirectory(out)) {
      Command.line("java").add(Path.of("b0/src/Build.java")).run();
    }
    Command.line("java")
        .add("--module-path", out.resolve("modules"))
        .add("--module", "com.greetings")
        .addAll(args)
        .run();
  }

  public static final class Command implements Runnable {
    public static Command line(String name) {
      return new Command(name, new ArrayList<>());
    }

    private final String name;
    private final List<String> arguments;

    Command(String name, List<String> arguments) {
      this.name = name;
      this.arguments = arguments;
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
}
