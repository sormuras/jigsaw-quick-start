package project;

import tools.Tool;
import tools.ToolCall;

public interface Builder extends Action {
    default void build() {
        var out = model().folders().out();
        // compile source files into class files
        ToolCall.of("javac")
                .add("-d", out.resolve("classes"))
                .add("--module-source-path", "src")
                .add("--module", "org.astro,com.greetings")
                .run();

        // compile class files into archive files
        var jar = Tool.of("jar");
        ToolCall.of(jar, "--create")
                .add("--file", out.resolve("modules", "com.greetings.jar"))
                .add("--main-class", "com.greetings.Main")
                .add("-C", out.resolve("classes", "com.greetings"), ".")
                .run();
        ToolCall.of(jar, "--create")
                .add("--file", out.resolve("modules", "org.astro.jar"))
                .add("-C", out.resolve("classes", "org.astro"), ".")
                .run();
    }
}
