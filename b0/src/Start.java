import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Start {
  public static void main(String... args) {
    var out = Path.of("b0", "out");
    if (!Files.isDirectory(out)) {
      Build.main();
    }
    java("--module-path", out.resolve("modules").toString(), "--module", "com.greetings");
  }

  private static void java(String... args) {
    System.out.println("| java " + String.join(" ", args));
    var executable = Path.of(System.getProperty("java.home"), "bin", "java");
    var builder = new ProcessBuilder(executable.toString());
    try {
      builder.command().addAll(List.of(args));
      var process = builder.inheritIO().start();
      var code = process.waitFor();
      if (code == 0) return;
      throw new RuntimeException("java returned non-zero exit code: " + code);
    } catch (Exception exception) {
      throw new RuntimeException("java failed.", exception);
    }
  }
}
