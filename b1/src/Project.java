import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

record Project(Path out) {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Path.of("b1", "out"));
  }

  void build() {
    var classes = out.resolve("classes");
    var modules = out.resolve("modules");

    Command.line("javac")
        .add("-d", classes)
        .add("--module-source-path", "src")
        .add("--module", "org.astro,com.greetings")
        .run();

    Command.line("jar")
        .add("--create")
        .add("--file", modules.resolve("com.greetings.jar"))
        .add("--main-class", "com.greetings.Main")
        .add("-C", classes.resolve("com.greetings"), ".")
        .run();
    Command.line("jar")
        .add("--create")
        .add("--file", modules.resolve("org.astro.jar"))
        .add("-C", classes.resolve("org.astro"), ".")
        .run();
  }

  void clean() {
    delete(out);
  }

  void start(String... args) {
    if (!Files.isDirectory(out)) {
      build();
    }
    Command.line("java")
        .add("--module-path", out.resolve("modules"))
        .add("--module", "com.greetings")
        .addAll(args)
        .run();
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
}
