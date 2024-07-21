package project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Cleaner extends Action {
  default void clean() {
    delete(model().folders().out());
  }

  private static void delete(Path path) {
    System.out.println("| delete " + path);
    var start = path.normalize().toAbsolutePath();
    if (Files.notExists(start)) return;
    try (var stream = Files.walk(start)) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
