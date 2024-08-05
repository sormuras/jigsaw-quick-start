package project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface Builder extends Action {
  default void build() {
    // compile source files into class files
    var classes = builderUsesDestinationDirectoryForClassFiles();
    run(
        "javac",
        "-d",
        classes.toString(),
        "--module-source-path",
        builderUsesModuleSourcePath(),
        "--module",
        String.join(",", builderUsesModuleNames()));

    // compile class files into archive files
    var modules = builderUsesDestinationDirectoryForModularJarFiles();
    for (var name : model().modules().names()) {
      var args = new ArrayList<String>();
      args.add("--create");
      args.add("--file");
      args.add(modules.resolve(name + ".jar").toString());
      args.addAll(builderUsesAdditionalJarArgumentsForModuleNamed(name));
      args.add("-C");
      args.add(classes.resolve(name).toString());
      args.add(".");
      run("jar", args.toArray(String[]::new));
    }
  }

  default Path builderUsesDestinationDirectoryForClassFiles() {
    return model().folders().out().resolve("classes");
  }

  default Path builderUsesDestinationDirectoryForModularJarFiles() {
    return model().folders().out().resolve("modules");
  }

  default String builderUsesModuleSourcePath() {
    return "src";
  }

  default List<String> builderUsesModuleNames() {
    return model().modules().names();
  }

  default List<String> builderUsesAdditionalJarArgumentsForModuleNamed(String moduleName) {
    return List.of();
  }
}
