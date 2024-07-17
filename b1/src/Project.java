import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

record Project(Path out) {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Path.of("b1", "out"));
  }

  void build() {
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
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules", "com.greetings.jar").toString(),
        "--main-class",
        "com.greetings.Main",
        "-C",
        out.resolve("classes", "com.greetings").toString(),
        ".");
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules", "org.astro.jar").toString(),
        "-C",
        out.resolve("classes", "org.astro").toString(),
        ".");
  }

  void clean() {
    delete(out);
  }

  void rebuild() {
    clean();
    build();
  }

  void start() {
    if (!Files.isDirectory(out)) {
      build();
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

  private static void delete(Path path) {
    System.out.println("| delete " + path);
    var start = path.normalize().toAbsolutePath();
    if (Files.notExists(start)) return;
    try (var stream = Files.walk(start)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static void run(String name, String... args) {
    System.out.println("| " + name + " " + String.join(" ", args));
    var tool = ToolProvider.findFirst(name);
    if (tool.isPresent()) {
      var code = tool.get().run(System.out, System.err, args);
      if (code == 0) return;
      throw new RuntimeException(name + " returned non-zero exit code: " + code);
    }
    var builder =
        new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", name).toString());
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
