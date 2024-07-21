import project.*;

record Project(Model model) implements Builder, Cleaner, Rebuilder, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }

  @Override
  public void start() {
    System.out.println("BEGIN");
    Starter.super.start();
    System.out.println("END.");
  }
}
