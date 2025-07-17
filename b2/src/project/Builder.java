package project;

public interface Builder extends Action {
  default void build() {
    var out = model().folders().out();
    var classes = out.resolve("classes");
    var modules = out.resolve("modules");

    Command.line("javac")
        .add("-d", classes)
        .add("--module-source-path", "src")
        .add("--module", "org.astro,com.greetings")
        .run();

    Command.line("jar")
        .add("--create")
        .add("--file", modules.resolve("com.greetings.jar"))
        .add("--main-class", "com.greetings.Main")
        .add("-C", classes.resolve("com.greetings"), ".")
        .run();
    Command.line("jar")
        .add("--create")
        .add("--file", modules.resolve("org.astro.jar"))
        .add("-C", classes.resolve("org.astro"), ".")
        .run();
  }
}
