# Extract Model

Extract data model and default actions from `record Project(...) {}` class into dedicated types.

```java
import project.*;

record Project(Model model) implements Builder, Cleaner, Starter {
  static Project ofCurrentWorkingDirectory() {
    return new Project(Model.of("b2"));
  }
}
```

## The Good

The `Project` record header definition has all interesting information.

Common properties are all declared in the `Model` record.
Subtypes storing more information are organized as nested record types.

Each action is defined in a dedicated interface declaration.
The name of the interface is derived from the action's method name: `build()` is defined in the `Builder` interface.

Access to a model instance via the inherited `Model model()` accessor defined the `Action` interface.

## The Bad

Project-specific information are no longer stored at the top-level.
