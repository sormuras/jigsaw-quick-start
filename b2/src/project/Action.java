package project;

import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

public interface Action {
    Model model();

    default void run(String name, String... args) {
        System.out.println("| " + name + " " + String.join(" ", args));
        var tool = ToolProvider.findFirst(name);
        if (tool.isPresent()) {
            var code = tool.get().run(System.out, System.err, args);
            if (code == 0) return;
            throw new RuntimeException(name + " returned non-zero exit code: " + code);
        }
        var builder =
                new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", name).toString());
        try {
            builder.command().addAll(List.of(args));
            var process = builder.inheritIO().start();
            var code = process.waitFor();
            if (code == 0) return;
            throw new RuntimeException(name + " returned non-zero exit code: " + code);
        } catch (Exception exception) {
            throw new RuntimeException(name + " failed.", exception);
        }
    }
}
