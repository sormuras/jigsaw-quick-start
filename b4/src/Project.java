import java.nio.file.Files;
import project.Builder;
import project.Cleaner;
import project.Model;
import project.Starter;
import project.Tool;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b4"));
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
