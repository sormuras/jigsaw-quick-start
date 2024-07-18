package tools;

/** Unchecked exception thrown when a tool could not be found. */
public class ToolNotFoundException extends RuntimeException {
  public ToolNotFoundException(String name) {
    super("Tool named '%s' not found".formatted(name));
  }
}
