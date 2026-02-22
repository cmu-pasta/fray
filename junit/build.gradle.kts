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
  testTask.doFirst {
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
    testTask.jvmArgs("-Dfray.debug=true")
  }
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
