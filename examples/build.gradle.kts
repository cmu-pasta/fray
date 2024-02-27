plugins {
    id("java")
}

group = "cmu.pasta.sfuzz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runExample") {
    mainClass.set("example.Main")
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED"))
}

tasks.register<JavaExec>("runWith") {
    var jvmti = project(":jvmti")
    var core = project(":core")
    var jdk = project(":jdk")
    classpath = files(tasks.jar)
    executable("${jdk.layout.buildDirectory.get().asFile}/jdk/bin/java")
    mainClass.set("example.Main")
    jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so")
    jvmArgs("-javaagent:${core.layout.buildDirectory.get().asFile}/libs/${core.name}-${core.version}.jar")
}

