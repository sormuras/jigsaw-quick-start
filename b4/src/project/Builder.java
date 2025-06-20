package project;

import java.util.List;
import project.Tool.Command;

public interface Builder extends Action {
  default void build() {
    var out = model().folders().out();
    // compile source files into class files
    var compileCommands =
        List.of(
            Command.of("javac")
                .add("-d", out.resolve("classes"))
                .add("--module-source-path", "src")
                .add("--module", "org.astro,com.greetings"));
    // compile class files into archive files
    var archiveCommands =
        List.of(
            Command.of("jar", "--create")
                .add("--file", out.resolve("modules", "com.greetings.jar"))
                .add("--main-class", "com.greetings.Main")
                .add("-C", out.resolve("classes", "com.greetings"), "."),
            Command.of("jar", "--create")
                .add("--file", out.resolve("modules", "org.astro.jar"))
                .add("-C", out.resolve("classes", "org.astro"), "."));

    var runner = Tool.Runner.ofSystem();
    compileCommands.forEach(runner::run);
    archiveCommands.parallelStream().forEach(runner::run);
  }
}
