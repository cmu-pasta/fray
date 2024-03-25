plugins {
    id("java")
}

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
    var jdk = project(":jdk")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("example.Main")
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED"))
}

tasks.register<JavaExec>("run") {
    val agentPath: String by rootProject.extra
    val jdk = project(":jdk")
    val instrumentation = project(":instrumentation")
    classpath = sourceSets["main"].runtimeClasspath
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    mainClass.set("cmu.pasta.sfuzz.core.MainKt")
    args = listOf("example.Main", "-o", "${layout.buildDirectory.get().asFile}/report", "--scheduler", "fifo")
    jvmArgs("-agentpath:$agentPath")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
    doFirst {
        // Printing the full command
        println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
    }
}

tasks.register<JavaExec>("runArithmeticProgBad") {
    var jdk = project(":jdk")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("example.ArithmeticProgBad")
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
}

tasks.register<JavaExec>("runArithmeticProgSfuzz") {
    val agentPath: String by rootProject.extra
    val jvmti = project(":jvmti")
    val jdk = project(":jdk")
    val instrumentation = project(":instrumentation")
    classpath = sourceSets["main"].runtimeClasspath
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    mainClass.set("cmu.pasta.sfuzz.core.MainKt")
    args = listOf("example.ArithmeticProgBad", "main", "-o", "${layout.buildDirectory.get().asFile}/report", "--scheduler", "fifo")
    jvmArgs("-agentpath:${agentPath}")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
    doFirst {
        // Printing the full command
        println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
    }
}
