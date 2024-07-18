package project;

public interface Rebuilder extends Action, Cleaner, Builder {
  default void rebuild() {
    clean();
    build();
  }
}
