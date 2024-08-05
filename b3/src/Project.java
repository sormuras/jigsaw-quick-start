import java.nio.file.Files;
import java.util.List;
import project.Builder;
import project.Cleaner;
import project.Model;
import project.Starter;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b3", "com.greetings", "org.astro"));
  }

  @Override
  public List<String> builderUsesAdditionalJarArgumentsForModuleNamed(String name) {
    return switch (name) {
      case "com.greetings" -> List.of("--main-class", "com.greetings.Main");
      case "org.astro" -> List.of("--module-version=1.0");
      default -> List.of();
    };
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
