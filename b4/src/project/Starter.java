package project;

public interface Starter extends Action {
  default void start() {
    Tool.of("java")
        .command()
        .add("--module-path", model().folders().out().resolve("modules"))
        .add("--module", "com.greetings")
        .run();
  }
}
