import project.Builder;
import project.Cleaner;
import project.Model;
import project.Starter;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }
}
