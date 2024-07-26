package project;

public interface Starter extends Action {
  default void start() {
    var out = model().folders().out();
    run("java","--module-path", out.resolve("modules").toString(), "--module", "com.greetings");
  }
}
