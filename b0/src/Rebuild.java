import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public class Rebuild {
  public static void main(String... args) {
    Command.line("java").add(Path.of("b0/src/Clean.java")).run();
    Command.line("java").add(Path.of("b0/src/Build.java")).run();
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
