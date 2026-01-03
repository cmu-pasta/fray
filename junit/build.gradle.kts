import java.util.regex.Pattern
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(libs.hamcrest)
  implementation(project(":core"))
  compileOnly(project(":runtime"))
  implementation(libs.kotlinx.serialization.json)
  compileOnly(libs.junit.platform.engine)
  compileOnly(libs.junit.platform.commons)
  compileOnly(libs.junit.jupiter.api)
  compileOnly(libs.junit4)
  compileOnly(libs.randomizedtesting.runner)
  testCompileOnly(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.junit.platform.testkit)
}

fun configureTestTask(testTask: Test) {
  testTask.useJUnitPlatform { includeEngines("junit-jupiter") }
  testTask.dependsOn(":instrumentation:jdk:build")
  val instrumentationTask =
      evaluationDependsOn(":instrumentation:agent").tasks.named("shadowJar").get()
  val jdk = project(":instrumentation:jdk")
  val jvmti = project(":jvmti")
  val instrumentation = instrumentationTask.outputs.files.first().absolutePath
  val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
  val javaExe = if (getCurrentOperatingSystem().toFamilyName() == "windows") "java.exe" else "java"
  println(instrumentation)
  testTask.classpath +=
      tasks.named("jar").get().outputs.files + files(configurations.runtimeClasspath)
  testTask.executable(
      "${jdk.layout.buildDirectory.get().asFile}${File.separator}java-inst${File.separator}bin${File.separator}$javaExe"
  )
  testTask.jvmArgs(
      "-agentpath:${jvmti.layout.buildDirectory.get().asFile}${File.separator}native-libs${File.separator}libjvmti.$soSuffix"
  )
  testTask.jvmArgs("-javaagent:$instrumentation")
  testTask.jvmArgs("-ea")
  testTask.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  testTask.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  testTask.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  testTask.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  testTask.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  testTask.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
}

tasks.test {
  configureTestTask(this)
  exclude("org/pastalab/fray/junit/internal/**")
}

tasks.register<Test>("debugTests") {
  val baseTest = tasks.named("test", Test::class.java).get()
  testClassesDirs = baseTest.testClassesDirs
  classpath = baseTest.classpath
  configureTestTask(this)
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.runtimeClasspath)
  into("${layout.buildDirectory.get().asFile}${File.separator}dependency")
}

tasks.withType<JavaExec> {
  dependsOn(":instrumentation:jdk:build")
  val instrumentationTask =
      evaluationDependsOn(":instrumentation:agent").tasks.named("shadowJar").get()
  val jdk = project(":instrumentation:jdk")
  val jvmti = project(":jvmti")
  val instrumentation = instrumentationTask.outputs.files.first().absolutePath
  val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
  val javaExe = if (getCurrentOperatingSystem().toFamilyName() == "windows") "java.exe" else "java"
  classpath += tasks.named("jar").get().outputs.files + files(configurations.runtimeClasspath)
  executable(
      "${jdk.layout.buildDirectory.get().asFile}${File.separator}java-inst${File.separator}bin${File.separator}$javaExe"
  )
  mainClass = "org.pastalab.fray.core.MainKt"
  jvmArgs(
      "-agentpath:${jvmti.layout.buildDirectory.get().asFile}${File.separator}native-libs${File.separator}libjvmti.$soSuffix"
  )
  jvmArgs("-javaagent:$instrumentation")
  jvmArgs("-ea")
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
}

tasks.register<JavaExec>("runFray") {
  var configPath = properties["configPath"] as String? ?: ""
  val extraArgs =
      when (val extraArgs = properties["extraArgs"]) {
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
  if (!File(configPath).isAbsolute) {
    configPath = System.getProperty("user.dir") + File.separator + configPath
  }
  args = listOf("--run-config", "json", "--config-path", configPath) + extraArgs
}
