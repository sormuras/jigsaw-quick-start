import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public class Start {
  public static void main(String... args) {
    var out = Path.of("b0", "out");
    if (!Files.isDirectory(out)) {
      run("java", Path.of("b0/src/Build.java").toString());
    }
    run("java", "--module-path=" + out.resolve("modules"), "--module=com.greetings");
  }

  public static void run(String name, String... args) {
    System.out.println("| " + name + " " + String.join(" ", args));
    var tool = ToolProvider.findFirst(name);
    if (tool.isPresent()) {
      var code = tool.get().run(System.out, System.err, args);
      if (code == 0) return;
      throw new RuntimeException(name + " returned non-zero exit code: " + code);
    }
    var program = Path.of(System.getProperty("java.home"), "bin", name);
    var builder = new ProcessBuilder(program.toString());
    try {
      builder.command().addAll(List.of(args));
      var process = builder.inheritIO().start();
      var code = process.waitFor();
      if (code == 0) return;
      throw new RuntimeException(name + " returned non-zero exit code: " + code);
    } catch (Exception exception) {
      throw new RuntimeException(name + " failed.", exception);
    }
  }
}
