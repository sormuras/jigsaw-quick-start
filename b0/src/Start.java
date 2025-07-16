import java.nio.file.Files;
import java.nio.file.Path;

public class Start {
  public static void main(String... args) {
    var out = Path.of("b0", "out");
    if (!Files.isDirectory(out)) {
      Build.main();
    }
    Build.run("java", "--module-path=" + out.resolve("modules"), "--module=com.greetings");
  }
}
