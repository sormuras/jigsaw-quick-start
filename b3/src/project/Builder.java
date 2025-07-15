package project;

import java.util.ArrayList;

public interface Builder extends Action {
  record Javac(ArrayList<String> arguments) implements Arguments {}

  record Jar(ArrayList<String> arguments, String module) implements Arguments {}

  default void build() {
    buildClasses();
    buildArchives();
  }

  default void buildClasses() {
    var javac = new Javac(new ArrayList<>());
    buildClassesWithDestinationDirectory(javac);
    buildClassesWithModuleSourcePath(javac);
    buildClassesWithModule(javac);
    run("javac", javac.arguments().toArray(String[]::new));
  }

  default void buildArchives() {
    var commands = new ArrayList<Jar>();
    for (var name : model().modules().names()) {
      var jar = new Jar(new ArrayList<>(), name);
      buildArchiveWithCreateMode(jar);
      buildArchiveWithFile(jar);
      buildArchiveWithContent(jar);
      commands.add(jar);
    }
    commands.stream().parallel().forEach(jar -> run("jar", jar.arguments().toArray(String[]::new)));
  }

  /// Compiles those source files in the named modules that are newer than the corresponding files
  /// in the output directory.
  ///
  /// `--module module-name (,module-name)*`
  default void buildClassesWithModule(Javac javac) {
    javac.add("--module", String.join(",", model().modules().names()));
  }

  /// Sets the destination directory (or class output directory) for class files.
  ///
  /// `-d directory`
  default void buildClassesWithDestinationDirectory(Javac javac) {
    javac.add("-d", model().folders().out().resolve("classes"));
  }

  /// Specifies where to find source files when compiling code in multiple modules.
  ///
  /// `--module-source-path module-source-path`
  default void buildClassesWithModuleSourcePath(Javac javac) {
    javac.add("--module-source-path", "src");
  }

  /// Creates the archive.
  ///
  /// `--create`
  default void buildArchiveWithCreateMode(Jar jar) {
    jar.add("--create");
  }

  /// Specifies the archive file name.
  ///
  /// `--file=FILE`
  default void buildArchiveWithFile(Jar jar) {
    var archive = model().folders().out().resolve("modules", jar.module() + ".jar");
    jar.add("--file", archive);
  }

  /// Changes into the specified directory and includes the files at the end of the command line.
  ///
  /// `-C directory files`
  default void buildArchiveWithContent(Jar jar) {
    var classes = model().folders().out().resolve("classes", jar.module());
    jar.add("-C", classes, ".");
  }
}
