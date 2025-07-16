package project;

public interface Starter extends Action {
  default void start() {
    Command.line("java")
        .add("--module-path", model().folders().out().resolve("modules"))
        .add("--module", "com.greetings")
        .run();
  }
}
