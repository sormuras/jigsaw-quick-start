import project.*;

record Project(Model model) implements Builder, Cleaner, Rebuilder, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b3"));
  }
}
