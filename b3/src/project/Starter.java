package project;

public interface Starter extends Action {
  default void start() {
    var out = model().folders().out();
    Command.line("java")
        .add("--module-path", out.resolve("modules"))
        .add("--module", "com.greetings")
        .run();
  }
}
