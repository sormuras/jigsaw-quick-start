# jigsaw-quick-start

This project uses the example from https://openjdk.org/projects/jigsaw/quick-start to show a Java-only setup:
all shell commands are transformed into platform-agnostic Java code.

```text
$ javac -d mods --module-source-path src $(find src -name "*.java")
$ jar --create --file=mlib/org.astro@1.0.jar --module-version=1.0 -C mods/org.astro .
$ jar --create --file=mlib/com.greetings.jar --main-class=com.greetings.Main -C mods/com.greetings .
$ java -p mlib -m com.greetings

Greetings world!
```

## Back to Basics

[Back to Basics](b0) or use command-line tools as primitives and express logic in Java.

Create a Java program for each action:
```
b0/src/
    Build.java
    Clean.java
    Rebuild.java
    Start.java
```

Implement the action logic, for example, `Start.java` could look like this:
```java
public class Start {
    public static void main(String... args) {
        var out = Path.of("b0", "out");
        if (!Files.isDirectory(out)) {
            Build.main(); // runs javac and jar to compile and package modules
        }
        java("--module-path=" + out.resolve("modules"), "--module=com.greetings");
    }
}
```

Run actions directly from within an IDE or on the shell via the `java` launcher:
```shell
java b0/src/Start.java
```

## Better Basics

[Better Basics](b1) or centralize common properties and methods.

Introduce `Project` record as a central place to organize common properties and methods.
```java
record Project(Path out) {
    void start() {
        if (!Files.isDirectory(out)) {
            build(); // runs javac and jar to compile and package modules
        }
        java("--module-path=" + out.resolve("modules"), "--module=com.greetings");
    }
}
```

Modify action programs to create and work on an instance of `Project`.
```java
public class Start {
    public static void main(String... args) {
        Project.ofCurrentWorkingDirectory().start();
    }
}
```


- [Extract Model](b2) Extract project model and actions.

```java
public class Start {
    public static void main(String... args) {
        Project.ofCurrentWorkingDirectory().start();
    }
}

record Project(Model model) implements Builder, Cleaner, Rebuilder, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }
}

public interface Starter extends Action, Builder {
    default void start() {
        // see above
    }
}
```

- [Tool Time](b3) Improve tooling support.
- [Tool's Trifecta](b4) ToolFinder - ToolRunner - ToolCall

- [External Tools](b5) Install external tools.
- [External Modules](b6) Import external modules.

- [Extension Points](b7) Introduce extension points for configuration.

