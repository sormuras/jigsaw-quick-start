import java.nio.file.Files;
import project.Builder;
import project.Cleaner;
import project.Command;
import project.Model;
import project.Starter;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b4", "com.greetings", "org.astro"));
  }

  @Override
  public void build() {
    Builder.super.build();
    try {
      Command.line("native-image")
          .add("--module-path", model.folders().out().resolve("modules"))
          .add("--module", "com.greetings")
          .run();
    } catch (RuntimeException exception) {
      System.err.println(exception.getMessage());
      System.err.println(exception.getCause().getMessage());
    }
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
  public void start(String... args) {
    if (!Files.isDirectory(model.folders().out())) {
      build();
    }
    Starter.super.start(args);
  }
}
