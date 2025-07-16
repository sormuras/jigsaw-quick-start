import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public class Build {
  public static void main(String... args) throws Exception {
    var out = Path.of("b0", "out");
    // compile source files into class files
    run(
        "javac",
        "-d",
        out.resolve("classes").toString(),
        "--module-source-path",
        "src",
        "--module",
        "org.astro,com.greetings");
    // compile class files into archive files
    Files.createDirectories(out.resolve("modules"));
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules").resolve("com.greetings.jar").toString(),
        "--main-class",
        "com.greetings.Main",
        "-C",
        out.resolve("classes").resolve("com.greetings").toString(),
        ".");
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules").resolve("org.astro.jar").toString(),
        "-C",
        out.resolve("classes").resolve("org.astro").toString(),
        ".");
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
