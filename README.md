# Jigsaw Quick-Start Revisited

This project uses the example from https://openjdk.org/projects/jigsaw/quick-start to show a Java-only build setup:
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

Introduce `Project` record as a central place to organize common properties and all action methods.
```java
record Project(Path out) {
    void start() {
        if (!Files.isDirectory(out)) { // Use instance record component `out`
            build(); // Invoke same-class sibling method `build()`
        }
        java("--module-path=" + out.resolve("modules"), "--module=com.greetings");
    }
    // Here be more methods: build(), clean(), rebuild(), ...
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

## Extract Model and Actions

[Extract Model](b2) or let the `Project` record compose desired traits.

Move common project properties into components of a `Model` record — which in turn is composed of nested records like `Folders`.

The `Action` interface defines an accessor to an instance of `Model`.
It may also host convenient helpers used by derived actions.

Wrap actions with custom code by overriding default methods.

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
  @Override
  public void start() {
      System.out.println("BEGIN");
      Starter.super.start();
      System.out.println("END.");
  }
}

public interface Starter extends Action, Builder {
    default void start() {
        var out = model().folders().out(); // Access nested record components
        if (!Files.isDirectory(out)) {     // Variable `out` is local again
            build();                       // Re-use default method in another trait: Builder.build()
        }
        java("--module-path=" + out.resolve("modules"), "--module=com.greetings");
    }
}
```

## Define extension points for configuration.

[Extension Points](b3) Introduce extension points for configuration.

Action interfaces (`Starter`) define a single method as their main purpose (`start()`).
Wrapping custom code around those action methods is achieved by overriding them.

Action interface may also define functions as overridable extension points to let a project configure the behaviour.

Names of such extension point functions should start with their interface name.
This naming convention helps to override right extension point and the prevents name clashes with other actions. 

```java
public interface Starter extends Action, Builder {
    default void start() {
        var out = model().folders().out();
        starterBuildBeforeStart();
        var module = starterUsesMainModule();
        java("--module-path=" + out.resolve("modules"), "--module=" + module);
    }
    
    default void starterBuildBeforeStart() {
        if (Files.isDirectory(model().folders().out())) return;
        build();
    }
    
    default String starterUsesMainModule() {
        return "com.greetings";
    }
}
```


## Tool Time

- [Tool Time](b4) Improve tooling support.

## Tool's Trifecta

- [Tool's Trifecta](b5) ToolFinder - ToolRunner - ToolCall

## Externals

- [External Tools](b6) Install external tools.
- [External Modules](b7) Import external modules.
