# Better Basics

Centralize common properties and methods in `record Project(...) {}` class.

## The Good

Separate the entry-point feature from build configuration and action details.

### Source of Interest

The `Project.java` file is the single point of interest in Java source form.

Share common properties used in multiple methods as record components.
```java
record Project(Path out) {
    // Component out can be accessed in all methods declared in Project
}
```

### Top Level Actions

Top-level Java programs still represent build-related actions.

Grace to "[JEP 512](https://openjdk.org/jeps/512): Compact Source Files and Instance Main Methods" those entry-points are one-liners.
For example, `Build.java`:

```java
void main() { Project.ofCurrentWorkingDirectory().build(); }
```

## The Bad

Too much code accumulating in `Project.java` file and mixture of data and behaviour.
