package project;

import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public interface Builder extends Action {
  default void build() {
    var out = model().folders().out();
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
