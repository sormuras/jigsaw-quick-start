package project;

import tools.ToolCall;
import java.nio.file.Files;

public interface Starter extends Action, Builder {
  default void start() {
    var out = model().folders().out();
    if (!Files.isDirectory(out)) {
      build();
    }
    ToolCall.of("java")
            .add("--module-path", out.resolve("modules"))
            .add("--module", "com.greetings")
            .run();
  }
}
