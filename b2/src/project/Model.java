package project;

import java.nio.file.Path;

public record Model(Folders folders) {
  public static Model of(String step) {
    var out = Path.of(step, "out");
    var folders = new Model.Folders(out);
    return new Model(folders);
  }

  public record Folders(Path out) {}
}
