package project;

import java.nio.file.Path;
import java.util.List;

public record Model(Folders folders, Modules modules) {
  public static Model of(String step, String... moduleNames) {
    var out = Path.of(step, "out");
    var folders = new Folders(out);
    var modules = new Modules(List.of(moduleNames));
    return new Model(folders, modules);
  }

  public record Folders(Path out) {}

  public record Modules(List<String> names) {}
}
