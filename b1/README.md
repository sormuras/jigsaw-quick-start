# Better Basics

Centralize common properties and methods in `record Project(...) {}` class.

## The Good

Separate the entry-point feature from build configuration and action details.

### Source of Interest

The `Project.java` file is the single point of interest in Java source form.

Share common properties used in multiple methods as record components.
```java
record Project(Path out) {
    // Component out can be access in all methods declared in Project
}
```

### Top Level Actions

Top-level Java programs are still representing the main actions.

With "Implicitly Declared Classes and Instance Main Methods" those entry-points will be almost one-liners soon.

```java
void main() { Project.ofCurrentWorkingDirectory().build(); }
```

## The Bad

Too much code accumulating in `Project.java` file and mixture of data and behaviour.
