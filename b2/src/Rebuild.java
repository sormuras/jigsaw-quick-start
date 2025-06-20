void main() {
    var project = Project.ofCurrentWorkingDirectory();
    project.clean();
    project.build();
}
