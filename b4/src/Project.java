import project.*;

import java.nio.file.Files;

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
