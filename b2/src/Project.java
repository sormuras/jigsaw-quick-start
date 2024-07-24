import project.*;

import java.nio.file.Files;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }

  void rebuild() {
    clean();
    build();
  }

  @Override
  public void start() {
    System.out.println("BEGIN");
    if (!Files.isDirectory(model.folders().out())) {
      build();
    }
    Starter.super.start();
    System.out.println("END.");
  }
}
