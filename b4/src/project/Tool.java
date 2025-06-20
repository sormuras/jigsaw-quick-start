package project;

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

public record Tool(Identifier identifier, ToolProvider provider) {
  public static Tool of(String name) {
    // Try with loading tool provider implementations using the system class loader first.
    var provider = ToolProvider.findFirst(name);
    if (provider.isPresent()) {
      return Tool.of(provider.get());
    }
    // Find executable tool program in JDK's binary directory.
    var program = Program.findJavaDevelopmentKitTool(name);
    if (program.isPresent()) {
      var namespace = "jdk.home/bin";
      var version = String.valueOf(Runtime.version().feature());
      return Tool.of(namespace, name, version, program.get());
    }
    // Find executable tool program in Graal's substrate binary directory.
    var substrate = Program.findSubstrateTool(name);
    if (substrate.isPresent()) {
      var namespace = "jdk.home/substrate";
      var version = String.valueOf(Runtime.version().feature());
      return Tool.of(namespace, name, version, substrate.get());
    }
    throw new NotFoundException(name);
  }

  public static Tool of(ToolProvider provider) {
    return new Tool(Identifier.of(provider), provider);
  }

  public static Tool of(String namespace, String name, String version, ToolProvider provider) {
    var identifier = new Identifier(namespace, name, Optional.ofNullable(version));
    return new Tool(identifier, provider);
  }

  public Command command(String... args) {
    return Command.of(this, args);
  }

  public void run(String... args) {
    command(args).run();
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

    public boolean matches(String string) {
      if (name.equals(string)) return true; // "javac"
      if (toNamespaceAndName().equals(string)) return true; // "jdk.compiler/javac"
      if (version.isPresent()) {
        if (toNameAndVersion().equals(string)) return true; // "javac@99"
        return toNamespaceAndNameAndVersion().equals(string); // "jdk.compiler/javac@99"
      }
      return false;
    }

    public String toNameAndVersion() {
      return version.map(version -> name + '@' + version).orElse(name);
    }

    public String toNamespaceAndName() {
      return namespace + '/' + name;
    }

    public String toNamespaceAndNameAndVersion() {
      return namespace + '/' + toNameAndVersion();
    }
  }

  public record Command(Carrier tool, List<String> arguments) implements Runnable {
    public sealed interface Carrier {
      String name();
      record Direct(Tool tool) implements Carrier {
        @Override
        public String name() {
          return tool.identifier().name();
        }
      }

      record Lookup(String name) implements Carrier {}
    }

    public static Command of(Tool tool, String... arguments) {
      return new Command(new Carrier.Direct(tool), List.of(arguments));
    }

    public static Command of(String name, String... arguments) {
      return new Command(new Carrier.Lookup(name), List.of(arguments));
    }

    public Command add(Object object) {
      return addAll(Stream.of(object));
    }

    public Command add(String key, Object value, Object... more) {
      return switch (more.length) {
        case 0 -> addAll(Stream.of(key, value));
        case 1 -> addAll(Stream.of(key, value, more[0]));
        case 2 -> addAll(Stream.of(key, value, more[0], more[1]));
        default -> addAll(Stream.concat(Stream.of(key, value), Stream.of(more)));
      };
    }

    public Command addAll(String... arguments) {
      return addAll((Object[]) arguments);
    }

    public Command addAll(Object... arguments) {
      return switch (arguments.length) {
        case 0 -> this;
        case 1 -> addAll(Stream.of(arguments[0]));
        case 2 -> addAll(Stream.of(arguments[0], arguments[1]));
        case 3 -> addAll(Stream.of(arguments[0], arguments[1], arguments[2]));
        default -> addAll(Stream.of(arguments));
      };
    }

    public Command addAll(Stream<?> arguments) {
      var head = this.arguments.stream();
      var tail = arguments.map(Object::toString);
      return new Command(tool, Stream.concat(head, tail).toList());
    }

    @Override
    public void run() {
      Runner.ofSystem().run(this);
    }

    public String toCommandLine() {
      return toCommandLine(" ");
    }

    public String toCommandLine(String delimiter) {
      return switch (arguments.size()) {
        case 0 -> tool.name();
        case 1 -> tool.name() + delimiter + arguments.getFirst();
        default -> tool.name() + delimiter + String.join(delimiter, arguments);
      };
    }
  }

  /**
   * An implementation of the tool provider interface running operating system programs.
   *
   * @param name the name of this tool program
   * @param command the list containing the executable program and its fixed arguments
   * @param processStarter the starter starts the process using the given process builder
   * @param processWaiter the waiter waits for the process to finish and returns an exit value
   * @param threadBuilder the builder used to start output and error stream globbing threads
   * @see ToolProvider#name()
   */
  public record Program(
      String name,
      List<String> command,
      ProcessStarter processStarter,
      ProcessWaiter processWaiter,
      Thread.Builder threadBuilder)
      implements ToolProvider {

    /**
     * {@return an instance of a tool program launching a Java application via the {@code java}
     * tool}
     *
     * <p>Example for using {@code java} with an array of fixed arguments:
     *
     * <pre>{@code
     * // java[.exe] --limit-modules java.base --list-modules
     * var base = ToolProgram.java("--limit-modules", "java.base");
     * var tool = Tool.of(base);
     * tool.run("--list-modules");
     * }</pre>
     *
     * @param args zero or more fixed arguments
     * @see <a href="https://docs.oracle.com/en/java/javase/22/docs/specs/man/java.html">The java
     *     Command</a>
     */
    public static Program java(String... args) {
      var name = "java";
      var tool = findJavaDevelopmentKitTool(name, args);
      if (tool.isPresent()) return tool.get();
      throw new NotFoundException(name);
    }

    /**
     * {@return an instance of a JDK tool program for the given name, if found}
     *
     * @param name the name of the JDK tool program to look up
     * @param args the fixed arguments
     * @see <a href="https://docs.oracle.com/en/java/javase/22/docs/specs/man/">Java® Development
     *     Kit Version 22 Tool Specifications</a>
     */
    public static Optional<Program> findJavaDevelopmentKitTool(String name, String... args) {
      var bin = Path.of(System.getProperty("java.home", ""), "bin");
      return findInFolder(name, bin, args);
    }

    public static Optional<Program> findSubstrateTool(String name, String... args) {
      var bin = Path.of(System.getProperty("java.home", ""), "lib", "svm", "bin");
      return findInFolder(name, bin, args);
    }

    /**
     * {@return an operating system program for the given name in the specified folder, if found}
     *
     * @param name the name of the operating system program to lookup
     * @param folder the directory to look up the name in
     * @param args the fixed arguments
     */
    public static Optional<Program> findInFolder(String name, Path folder, String... args) {
      if (!Files.isDirectory(folder)) return Optional.empty();
      var win = System.getProperty("os.name", "").toLowerCase().startsWith("win");
      var file = name + (win && !name.endsWith(".exe") ? ".exe" : "");
      try {
        var path = folder.resolve(file);
        return findExecutable(name, path, args);
      } catch (InvalidPathException exception) {
        return Optional.empty();
      }
    }

    /**
     * {@return an operating system program for the given file path, if it is executable}
     *
     * @param name the name of the operating system program to lookup
     * @param file the file to look up the name in
     * @param args the fixed arguments
     * @see Files#isExecutable(Path)
     */
    public static Optional<Program> findExecutable(String name, Path file, String... args) {
      if (!Files.isExecutable(file)) return Optional.empty();
      var command = new ArrayList<String>();
      command.add(file.toString());
      command.addAll(List.of(args));
      return Optional.of(new Program(name, List.copyOf(command)));
    }

    public Program(String name, List<String> command) {
      this(name, command, ProcessBuilder::start, Process::waitFor, Thread.ofVirtual());
    }

    public Program withProcessStarter(ProcessStarter processStarter) {
      return new Program(name, command, processStarter, processWaiter, threadBuilder);
    }

    public Program withProcessWaiter(ProcessWaiter processWaiter) {
      return new Program(name, command, processStarter, processWaiter, threadBuilder);
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... arguments) {
      var processBuilder = new ProcessBuilder(new ArrayList<>(command));
      processBuilder.command().addAll(List.of(arguments));
      try {
        var process = processStarter.start(processBuilder);
        threadBuilder.name(name + "-out").start(new LinePrinter(process.getInputStream(), out));
        threadBuilder.name(name + "-err").start(new LinePrinter(process.getErrorStream(), err));
        return process.isAlive() ? processWaiter().waitFor(process) : process.exitValue();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        return -1;
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return 1;
      }
    }

    @FunctionalInterface
    public interface ProcessStarter {
      Process start(ProcessBuilder builder) throws IOException;
    }

    @FunctionalInterface
    public interface ProcessWaiter {
      int waitFor(Process process) throws InterruptedException;
    }

    private record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
      @Override
      public void run() {
        new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
      }
    }
  }

  /**
   * A finder of tools.
   *
   * <p>Usage example:
   *
   * <pre>{@code
   * ToolFinder.compose(
   *     ToolFinder.of("jar", "javac", "javadoc"),
   *     ToolFinder.of("java", "jfr")
   * )
   * }</pre>
   */
  @FunctionalInterface
  public interface Finder {
    /** {@return a list of tool instances of this finder, possibly empty} */
    List<Tool> tools();

    /**
     * {@return an instance of a tool for the given tool-identifying name}
     *
     * @param name the name of the tool to lookup
     */
    default Optional<Tool> find(String name) {
      return tools().stream().filter(tool -> tool.identifier().matches(name)).findFirst();
    }

    /**
     * {@return an instance of a tool for the given tool-identifying name}
     *
     * @param name the name of the tool to lookup
     * @throws NotFoundException when a tool could not be found the given name
     */
    default Tool get(String name) {
      var tool = find(name);
      if (tool.isPresent()) return tool.get();
      throw new NotFoundException("Tool not found for name: " + name);
    }

    /**
     * {@return a tool finder composed all tools specified by their names}
     *
     * @param tools the names of the tools to be looked-up
     * @throws NotFoundException if any tool could not be found
     */
    static Finder of(String... tools) {
      return of(Stream.of(tools).map(Tool::of).toArray(Tool[]::new));
    }

    /**
     * {@return a tool finder composed of a sequence of zero or more tools}
     *
     * @param tools the array of tools
     */
    static Finder of(Tool... tools) {
      return new DefaultFinder(List.of(tools));
    }

    static Finder of(ModuleFinder finder, String... roots) {
      var parentClassLoader = Finder.class.getClassLoader();
      var parentModuleLayer = ModuleLayer.boot();
      var parents = List.of(parentModuleLayer.configuration());
      var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
      var layers = List.of(parentModuleLayer);
      var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
      return of(controller.layer());
    }

    static Finder of(ModuleLayer layer) {
      var tools =
          ServiceLoader.load(layer, ToolProvider.class).stream()
              .filter(service -> service.type().getModule().getLayer() == layer)
              .map(ServiceLoader.Provider::get)
              .map(Tool::of)
              .toList();
      return new DefaultFinder(tools);
    }

    static Finder ofSystem() {
      return new SystemFinder();
    }

    /**
     * {@return a tool finder that is composed of a sequence of zero or more tool finders}
     *
     * @param finders the array of tool finders
     */
    static Finder compose(Finder... finders) {
      return new CompositeFinder(List.of(finders));
    }

    record CompositeFinder(List<Finder> finders) implements Finder {

      public CompositeFinder {
        finders = List.copyOf(finders);
      }

      @Override
      public List<Tool> tools() {
        return finders.stream().flatMap(finder -> finder.tools().stream()).toList();
      }

      @Override
      public Optional<Tool> find(String name) {
        for (var finder : finders) {
          var tool = finder.find(name);
          if (tool.isPresent()) return tool;
        }
        return Optional.empty();
      }
    }

    record DefaultFinder(List<Tool> tools) implements Finder {
      public DefaultFinder {
        tools = List.copyOf(tools);
      }
    }

    record SystemFinder() implements Finder {
      @Override
      public List<Tool> tools() {
        // TODO Load tool providers using the context class loader.
        // TODO List tool programs of the current JDK's bin folder.
        return List.of();
      }

      @Override
      public Optional<Tool> find(String name) {
        try {
          return Optional.of(Tool.of(name));
        } catch (NotFoundException exception) {
          return Optional.empty();
        }
      }
    }
  }

  /** Recorded data of a tool run. */
  public record Result(Command command, Tool tool, int code, String out, String err) {}

  /**
   * A runner of tool commands.
   *
   * <p>Usage example:
   *
   * <pre>{@code
   * Tool.Runner.ofSystem().run("java", "--version");
   * }</pre>
   */
  @FunctionalInterface
  public interface Runner {
    /**
     * {@return an instance of the default tool runner using the given tool finder}
     *
     * @param finder the finder instance to be used for finding tools by name
     */
    static Runner of(Finder finder) {
      return new DefaultRunner(finder);
    }

    /** {@return an instance of the default tool runner using the system tool finder} */
    static Runner ofSilence() {
      return new DefaultRunner(Finder.ofSystem(), System.Logger.Level.OFF, DefaultRunner.Flag.SILENT);
    }

    /** {@return an instance of the default tool runner using the system tool finder} */
    static Runner ofSystem() {
      class SystemRunner {
        static final Runner SINGLETON = Runner.of(Finder.ofSystem());
      }
      return SystemRunner.SINGLETON;
    }

    Result run(Command command);

    default void log(System.Logger.Level level, String message) {
      System.out.printf("[%s] %s".formatted(level.name().charAt(0), message));
    }

    default Result run(Tool tool, String... args) {
      return run(Command.of(tool).addAll(args));
    }

    default Result run(Tool tool, UnaryOperator<Command> args) {
      return run(args.apply(Command.of(tool)));
    }

    default Result run(String tool, String... args) {
      return run(Command.of(tool).addAll(args));
    }

    default Result run(String tool, UnaryOperator<Command> args) {
      return run(args.apply(Command.of(tool)));
    }

    /** Extendable tool runner implementation. */
    class DefaultRunner implements Runner {
      public enum Flag {
        SILENT
      }

      protected final Finder finder;
      protected final System.Logger.Level threshold;
      protected final Set<Flag> flags;

      public DefaultRunner(Flag... flags) {
        this(Finder.ofSystem(), System.Logger.Level.INFO, flags);
      }

      public DefaultRunner(Finder finder, Flag... flags) {
        this(finder, System.Logger.Level.INFO, flags);
      }

      public DefaultRunner(Finder finder, System.Logger.Level threshold, Flag... flags) {
        this.finder = finder;
        this.threshold = threshold;
        this.flags =
            switch (flags.length) {
              case 0 -> EnumSet.noneOf(Flag.class);
              case 1 -> EnumSet.of(flags[0]);
              default -> EnumSet.of(flags[0], flags);
            };
      }

      public final boolean silent() {
        return flags.contains(Flag.SILENT);
      }

      @Override
      public Result run(Command command) {
        announce(command);
        var event = new FlightRecorderEvent.ToolRunEvent();
        try {
          var tool = computeToolInstance(command);
          var args = computeArgumentsArray(command);
          var out = new StringPrintWriterMirror(computePrintWriter(System.Logger.Level.INFO));
          var err = new StringPrintWriterMirror(computePrintWriter(System.Logger.Level.ERROR));
          var provider = tool.provider();

          event.name = command.tool().name();
          event.tool = provider.getClass();
          event.args = String.join(" ", args);

          Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
          try {
            event.begin();
            event.code = provider.run(out, err, args);
          } catch (RuntimeException unchecked) {
            event.code = Integer.MIN_VALUE;
            throw unchecked;
          } finally {
            event.end();
            event.out = out.toString();
            event.err = err.toString();
          }

          var result = new Result(command, tool, event.code, event.out, event.err);
          verify(result);

          return result;
        } finally {
          event.commit();
        }
      }

      @Override
      public void log(System.Logger.Level level, String message) {
        // TODO Fire flight recorder event.
        if (silent()) return;
        var severity = level.getSeverity();
        if (severity < threshold.getSeverity()) return;
        if (severity < System.Logger.Level.ERROR.getSeverity()) {
          System.out.println(message);
        } else {
          System.err.println(message);
        }
      }

      protected void announce(Command command) {
        log(System.Logger.Level.INFO, "| " + command.toCommandLine());
      }

      protected PrintWriter computePrintWriter(System.Logger.Level level) {
        if (silent() || level == System.Logger.Level.OFF) {
          return new PrintWriter(Writer.nullWriter());
        }
        var severity = level.getSeverity();
        var stream = severity < System.Logger.Level.ERROR.getSeverity() ? System.out : System.err;
        return new PrintWriter(stream, true);
      }

      protected Tool computeToolInstance(Command command) {
        return switch (command.tool()) {
          case Command.Carrier.Lookup(String name) -> finder.get(name);
          case Command.Carrier.Direct(Tool tool) -> tool;
        };
      }

      protected String[] computeArgumentsArray(Command command) {
        return command.arguments().toArray(String[]::new);
      }

      protected void verify(Result result) {
        var code = result.code();
        if (code == 0) return;
        var name = result.command().tool().name();
        throw new RuntimeException("%s finished with exit code %d".formatted(name, code));
      }

      private static class StringPrintWriter extends PrintWriter {
        StringPrintWriter() {
          super(new StringWriter());
        }

        @Override
        public String toString() {
          return super.out.toString().stripTrailing();
        }
      }

      private static class StringPrintWriterMirror extends StringPrintWriter {
        private final PrintWriter other;

        StringPrintWriterMirror(PrintWriter other) {
          this.other = other;
        }

        @Override
        public void flush() {
          super.flush();
          other.flush();
        }

        @Override
        public void write(int c) {
          super.write(c);
          other.write(c);
        }

        @Override
        public void write(char[] buf, int off, int len) {
          super.write(buf, off, len);
          other.write(buf, off, len);
        }

        @Override
        public void write(String s, int off, int len) {
          super.write(s, off, len);
          other.write(s, off, len);
        }

        @Override
        public void println() {
          super.println();
          other.println();
        }
      }
    }
  }

  @Category("Bach")
  public abstract static sealed class FlightRecorderEvent extends Event {
    @Label("Tool Run")
    @Name("Bach.ToolRun")
    public static final class ToolRunEvent extends FlightRecorderEvent {
      @Label("Tool")
      public Class<?> tool;

      @Label("Name")
      public String name;

      @Label("Arguments")
      public String args;

      @Label("Exit Code")
      public int code;

      @Label("Output")
      public String out;

      @Label("Errors")
      public String err;
    }
  }

  /** Unchecked exception thrown when a tool could not be found. */
  public static class NotFoundException extends RuntimeException {
    public NotFoundException(String name) {
      super("Tool named '%s' not found".formatted(name));
    }
  }
}
