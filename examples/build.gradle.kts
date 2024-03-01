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
    implementation(project(":core"))
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
    doFirst {
      println(executable + " " + jvmArgs!!.joinToString(" ") + " -cp " + classpath.getAsPath() + " cmu.pasta.sfuzz.core.MainKt")
    }
    var jvmti = project(":jvmti")
    var jdk = project(":jdk")
    var instrumentation = project(":instrumentation")
    classpath = sourceSets["main"].runtimeClasspath
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    mainClass.set("cmu.pasta.sfuzz.core.MainKt")
    args = listOf("example.Main", "-o", "${layout.buildDirectory.get().asFile}/report")
    jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
}

