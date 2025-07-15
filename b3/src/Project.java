import java.nio.file.Files;
import project.Builder;
import project.Cleaner;
import project.Model;
import project.Starter;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b3", "com.greetings", "org.astro"));
  }

  @Override
  public void buildArchiveWithFile(Jar jar) {
    Builder.super.buildArchiveWithFile(jar); // --file filename.jar
    switch (jar.module()) {
      case "com.greetings" -> jar.add("--main-class", "com.greetings.Main");
      case "org.astro" -> jar.add("--module-version", "1.0");
    }
  }

  @Override
  public void start() {
    var out = model.folders().out();
    if (!Files.isDirectory(out)) {
      build();
    }
    Starter.super.start(); // run("java", "--module-path", out.resolve("modules") ...
  }
}
