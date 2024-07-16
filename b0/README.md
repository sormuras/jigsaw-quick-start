# b0 - Back to Basic

Fight for simplicity: express logic in plain Java and use tools provided by the Java Development Kit.

## b0 - The Good

- For each action there's a dedicated Java program, e.g. `Build!` ➡️ `b0/src/Build.java`

- Need a new action? Create another Java program! `b0/src/MyAction.java`
- Composite actions? Reference other Java programs! `b0/src/Rebuild.java`

- Java programs can be launched from the console, e.g. `java b0/src/Start.java` (JEP 330, JEP 458)
- Press Play (on Tape) in IDE(A)s, no extra plugins required

## b0 - The Ugly

- Common properties spread over multiple files, e.g. `var out = Path.of("b0", "out");`
