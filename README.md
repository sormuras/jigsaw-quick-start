# Jigsaw Quick-Start Revisited

This project uses the example from the [Project Jigsaw Quick-Start](https://openjdk.org/projects/jigsaw/quick-start) guide to show a 100% Java build setup:
all shell commands are transformed into platform-agnostic Java code.

```text
$ javac -d mods --module-source-path src $(find src -name "*.java")
$ jar --create --file=mlib/org.astro@1.0.jar --module-version=1.0 -C mods/org.astro .
$ jar --create --file=mlib/com.greetings.jar --main-class=com.greetings.Main -C mods/com.greetings .
$ java -p mlib -m com.greetings

Greetings world!
```

## Back to Basics

Use command-line tools as primitives and express logic in Java programs.

Create a Java program for each action:
```
b0/src/
    Build.java
    Clean.java
    Rebuild.java
    Start.java
```

For example, `Start.java` can be implemented like this:
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

This is possible due to "[JEP 330](https://openjdk.org/jeps/330): Launch Single-File Source-Code Programs" introduced in Java 11.

## Better Basics

With the help of "[JEP 458](https://openjdk.org/jeps/458): Launch Multi-File Source-Code Programs" it is possible to centralize common properties and methods.

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

With "[JEP 477](https://openjdk.org/jeps/477): Implicitly Declared Classes and Instance Main Methods" those entry-point programs like `Start.java` can be reduced to:
```java
main() { Project.ofCurrentWorkingDirectory().start(); }
```

## Extract Model and Actions

Let the `Project` record compose desired traits.

Move common project properties into components of a `Model` record — which in turn is composed of nested records like `Folders`.

The `Action` interface defines an accessor to an instance of `Model`.

Wrap actions with custom code by overriding default methods.

```java
record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }
    @Override
    public void start() {
      if (!Files.isDirectory(model.folders().out())) {
          build(); // Builder.this.build();
      }
      Starter.super.start();
    }
}

public interface Starter extends Action, Builder {
    default void start() {
        var out = model().folders().out(); // Access nested record components
        if (!Files.isDirectory(out)) {     // Variable `out` is local again
            build();                       // Re-use method from another trait: Builder.build()
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
