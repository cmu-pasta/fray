import java.util.regex.Pattern
plugins {
  id("java")
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.junit.vintage:junit-vintage-engine:5.10.2")
  implementation("org.junit.platform:junit-platform-console-standalone:1.10.2")
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
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  doFirst {
    // Printing the full command
    println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
  }
}

tasks.register<JavaExec>("runFray") {
  val configPath = properties["configPath"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> {
      val pattern = Pattern.compile("""("[^"]+"|\S+)""")
      val matcher = pattern.matcher(extraArgs)
      val result = mutableListOf<String>()
      while (matcher.find()) {
        result.add(matcher.group(1).replace("\"", ""))
      }
      result
    }
    else -> emptyList()
  }
  args = listOf("--run-config", "json", "--config-path", configPath) + extraArgs
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

tasks.register<JavaExec>("runJC") {
  val cp = properties["classpath"] as String? ?: ""
  val main = properties["mainClass"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  classpath += files(cp.split(":"))
  args = listOf(main, "main", "-o", "${layout.buildDirectory.get().asFile}/report", "--logger", "csv", "--iter", "10000", "-s", "10000000") + extraArgs
}

fun resolveClasspath(classpath: String): List<String> {
  return classpath.split(":").flatMap { path ->
    if (path.contains("*")) {
      val dir = File(path.substringBeforeLast("/"))
      val pattern = path.substringAfterLast("/")
      dir.listFiles { _, name -> name.matches(Regex(pattern.replace("*", ".*"))) }
          ?.map { it.absolutePath }
        ?: emptyList()
    } else {
      listOf(File(path).absolutePath)
    }
  }
}

tasks.register<JavaExec>("runJUnit") {
  val mainClass = "org.junit.platform.console.ConsoleLauncher"
  val method = "main"
  val testCase = properties["testCase"] as String? ?: ""
  val classPath = resolveClasspath(properties["classpath"] as String? ?: "").map {
    "-cp $it"
  }.joinToString(" ")
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  args = listOf(mainClass, method, "-a", "-m $testCase $classPath --details=verbose",
      "-o", "${layout.buildDirectory.get().asFile}/report", "--logger", "json", "--iter", "10000", "-s", "990000000") + extraArgs
}

tasks.register<JavaExec>("runSCT") {
  val cp = properties["classpath"] as String? ?: ""
  val main = properties["mainClass"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  classpath += files(cp.split(":"))
  args = listOf("cmu.pasta.sfuzz.benchmark.$main", "main",  "-o", "${layout.buildDirectory.get().asFile}/report", "--logger", "csv", "--iter", "10000") + extraArgs
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
