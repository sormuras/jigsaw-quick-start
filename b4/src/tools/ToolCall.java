package tools;

import java.util.List;
import java.util.stream.Stream;

public record ToolCall(Carrier tool, List<String> arguments) {
    public sealed interface Carrier {
        record Direct(Tool tool) implements Carrier {}
        record Lookup(String name) implements Carrier {}
    }

    public static ToolCall of(Tool tool, String... arguments) {
        return new ToolCall(new Carrier.Direct(tool), List.of(arguments));
    }

    public static ToolCall of(String name, String... arguments) {
        return new ToolCall(new Carrier.Lookup(name), List.of(arguments));
    }

    public ToolCall add(Object object) {
        return addAll(Stream.of(object));
    }

    public ToolCall add(String key, Object value, Object... more) {
        return switch (more.length) {
            case 0 -> addAll(Stream.of(key, value));
            case 1 -> addAll(Stream.of(key, value, more[0]));
            case 2 -> addAll(Stream.of(key, value, more[0], more[1]));
            default -> addAll(Stream.concat(Stream.of(key, value), Stream.of(more)));
        };
    }

    public ToolCall addAll(String... arguments) {
        return addAll((Object[]) arguments);
    }

    public ToolCall addAll(Object... arguments) {
        return switch (arguments.length) {
            case 0 -> this;
            case 1 -> addAll(Stream.of(arguments[0]));
            case 2 -> addAll(Stream.of(arguments[0], arguments[1]));
            case 3 -> addAll(Stream.of(arguments[0], arguments[1], arguments[2]));
            default -> addAll(Stream.of(arguments));
        };
    }

    public ToolCall addAll(Stream<?> arguments) {
        var head = this.arguments.stream();
        var tail = arguments.map(Object::toString);
        return new ToolCall(tool, Stream.concat(head, tail).toList());
    }

    public void run() {
        var tool = switch (this.tool) {
          case Carrier.Direct direct -> direct.tool();
          case Carrier.Lookup lookup -> Tool.of(lookup.name());
        };
        var name = tool.identifier().name();
        var args = arguments.toArray(String[]::new);

        System.out.println("| " + name + " " + String.join(" ", args));
        var code = tool.provider().run(System.out, System.err, args);
        if (code == 0) return;
        throw new RuntimeException(name + " returned non-zero exit code: " + code);
    }
}
