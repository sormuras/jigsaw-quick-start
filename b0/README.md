# Back to Basics

Use command-line tools as primitives and express build logic in plain Java.

## The Good

Quickly get started on any platform.

### Pure Java Programs

For each action there's a dedicated Java program.

_Build!_ ➡️ `Build.java`

- Need a new action? Create another Java program! `MyAction.java`
- Composite actions? Reference other Java programs! `Rebuild.java` = `Clean.main(); Build.main()`

Java programs can be launched from the console via `java Build.java`.
Press Play ▶️ in IDE(A)s to launch Java programs — no extra plugins required.

### Tools Tools Tools

Call command-line tools to build your Java product.

- Want to rerun a tool call? Copy and paste the call on your shell! `| javac ...`
- Tune tool call arguments? Modify the hosting Java program! `run("javac", ...)`

The Java Development Kit provides tools to build Java products — no extra tools required.
Java has the `ToolProvider` SPI to invoke tools without necessarily starting a new VM.

## The Bad

Java Programs
- Common properties spread over multiple files, e.g. `var out = Path.of("b0", "out");`
- Primitive tool call representation, e.g. `void run(String name, String[] args) { ... }`

Tools
- External tools
- External modules

## The Ugly

With great power (100% Java) comes great responsibility (maintainable code).

Where's the difference between code used in production, code used for testing, and code used to build products?
