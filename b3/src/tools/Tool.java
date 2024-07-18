package tools;

import java.util.Optional;
import java.util.spi.ToolProvider;

public record Tool(Identifier identifier, ToolProvider provider) {
    public static Tool of(String name) {
        // Try with loading tool provider implementations using the system class loader first.
        var provider = ToolProvider.findFirst(name);
        if (provider.isPresent()) {
            return Tool.of(provider.get());
        }
        // Find executable tool program in JDK's binary directory.
        var program = ToolProgram.findJavaDevelopmentKitTool(name);
        if (program.isPresent()) {
            var namespace = "jdk.home/bin";
            var version = String.valueOf(Runtime.version().feature());
            return Tool.of(namespace, name, version, program.get());
        }
        throw new ToolNotFoundException(name);
    }

    public static Tool of(ToolProvider provider) {
        return new Tool(Identifier.of(provider), provider);
    }

    public static Tool of(String namespace, String name, String version, ToolProvider provider) {
        var identifier = new Identifier(namespace, name, Optional.ofNullable(version));
        return new Tool(identifier, provider);
    }

    public record Identifier(String namespace, String name, Optional<String> version) {
        public static Identifier of(ToolProvider provider) {
            var namespace = computeNamespaceOf(provider);
            var name = provider.name();
            var version = computeVersionOf(provider);
            return new Identifier(namespace, name, version);
        }

        private static String computeNamespaceOf(Object object) {
            var type = object.getClass();
            var module = type.getModule();
            return module.isNamed() ? module.getName() : type.getPackageName();
        }

        private static Optional<String> computeVersionOf(Object object) {
            var type = object.getClass();
            var module = type.getModule();
            var moduleVersion = module.getDescriptor().version();
            if (moduleVersion.isPresent()) return moduleVersion.map(Object::toString);
            var meta = type.getPackage();
            if (meta == null) return Optional.empty();
            var implementationVersion = meta.getImplementationVersion();
            if (implementationVersion != null) return Optional.of(implementationVersion);
            return Optional.ofNullable(meta.getSpecificationVersion());
        }
    }
}
