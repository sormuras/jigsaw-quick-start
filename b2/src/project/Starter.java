package project;

import java.nio.file.Files;

public interface Starter extends Action, Builder {
  default void start(String... args) {
      var out = model().folders().out();
      if (!Files.isDirectory(out)) {
          build();
      }
      Command.line("java")
              .add("--module-path", out.resolve("modules"))
              .add("--module", "com.greetings")
              .addAll(args)
              .run();
  }
}
