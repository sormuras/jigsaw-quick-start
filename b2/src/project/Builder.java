package project;

public interface Builder extends Action {
  default void build() {
    var out = model().folders().out();
    // compile source files into class files
    run(
        "javac",
        "-d",
        out.resolve("classes").toString(),
        "--module-source-path",
        "src",
        "--module",
        "org.astro,com.greetings");
    // compile class files into archive files
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules", "com.greetings.jar").toString(),
        "--main-class",
        "com.greetings.Main",
        "-C",
        out.resolve("classes", "com.greetings").toString(),
        ".");
    run(
        "jar",
        "--create",
        "--file",
        out.resolve("modules", "org.astro.jar").toString(),
        "-C",
        out.resolve("classes", "org.astro").toString(),
        ".");
  }
}
