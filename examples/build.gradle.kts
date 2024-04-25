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

tasks.withType<JavaExec> {
  val agentPath: String by rootProject.extra
  val jdk = project(":jdk")
  val instrumentation = project(":instrumentation")
  classpath = sourceSets["main"].runtimeClasspath
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  mainClass = "cmu.pasta.fray.core.MainKt"
  jvmArgs("-agentpath:$agentPath")
  jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
  jvmArgs("-ea")
  doFirst {
    // Printing the full command
    println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
  }
}


tasks.compileJava {
    options.compilerArgs.addAll(listOf("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED"))
}

tasks.register<JavaExec>("runExample") {
    args = listOf("example.Main", "-o", "${layout.buildDirectory.get().asFile}/report", "--scheduler", "fifo")
}

tasks.register<JavaExec>("replay") {
  val cp = properties["classpath"] as String? ?: ""
  val main = properties["mainClass"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  classpath += files(cp)
  args = listOf("cmu.pasta.fray.benchmark.$main", "main", "-o", "/tmp/report", "--logger", "csv", "--iter", "10000") + extraArgs
}

tasks.register<JavaExec>("runSCT") {
  val cp = properties["classpath"] as String? ?: ""
  val main = properties["mainClass"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  classpath += files(cp)
  args = listOf("cmu.pasta.sfuzz.benchmark.$main", "main", "-o", "${layout.buildDirectory.get().asFile}/report", "--logger", "csv", "--iter", "10000") + extraArgs
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
    mainClass.set("cmu.pasta.fray.core.MainKt")
    args = listOf("example.ArithmeticProgBad", "main", "-o", "${layout.buildDirectory.get().asFile}/report", "--scheduler", "fifo")
    jvmArgs("-agentpath:${agentPath}")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
    doFirst {
        // Printing the full command
        println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
    }
}
