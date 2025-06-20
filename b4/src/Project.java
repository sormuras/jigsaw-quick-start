import java.nio.file.Files;
import project.Builder;
import project.Cleaner;
import project.Model;
import project.Starter;
import project.Tool;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b4", "com.greetings", "org.astro"));
  }

  @Override
  public void build() {
    Builder.super.build();
    try {
      Tool.of("native-image")
              .command()
              .add("--module-path", model.folders().out().resolve("modules"))
              .add("--module", "com.greetings")
              .run();
    } catch (Tool.NotFoundException exception) {
      System.err.println();
    }
  }

  @Override
  public void start() {
    var out = model.folders().out();
    if (!Files.isDirectory(out)) {
      build();
    }
    Starter.super.start();
  }
}
