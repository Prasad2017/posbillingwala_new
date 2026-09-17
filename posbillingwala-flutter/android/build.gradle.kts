allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// firebase_messaging still calls Android APIs those SDKs deprecated.
// Hide its javac deprecation noise; do not edit Pub Cache.
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        if (project.name != "firebase_messaging") {
            return@configureEach
        }
        options.isDeprecation = false
        doFirst {
            options.compilerArgs.removeAll { arg ->
                arg == "-deprecation" || arg == "-Xlint:deprecation"
            }
            if ("-Xlint:-deprecation" !in options.compilerArgs) {
                options.compilerArgs.add("-Xlint:-deprecation")
            }
            if ("-nowarn" !in options.compilerArgs) {
                options.compilerArgs.add("-nowarn")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
