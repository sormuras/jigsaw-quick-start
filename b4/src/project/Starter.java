package project;

public interface Starter extends Action {
  default void start() {
    run(
        "java",
        java ->
            java.add("--module-path", model().folders().out().resolve("modules"))
                .add("--module", "com.greetings"));
  }
}
