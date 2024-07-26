package project;

import tools.ToolCall;

public interface Starter extends Action {
  default void start() {
    var out = model().folders().out();
    ToolCall.of("java")
            .add("--module-path", out.resolve("modules"))
            .add("--module", "com.greetings")
            .run();
  }
}
