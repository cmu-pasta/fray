import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.0.0"
  id("com.gradleup.shadow")
}

dependencies {
  compileOnly(project(":runtime"))
  implementation(project(":instrumentation:agent", configuration = "shadow"))
  api(project(":rmi"))
  api("com.github.ajalt.clikt:clikt:4.2.2")
  runtimeOnly(kotlin("reflect"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation(project(":runtime"))
  testImplementation(project(":instrumentation:base"))
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

tasks.named("build") {
  dependsOn("shadowJar")
  finalizedBy("genRunner")
}

tasks.register("genRunner") {
  val currentProject = project
  val currentRootProject = rootProject
  val currentVersion = version
  val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
  doLast {
    val binDir = "${currentRootProject.projectDir.absolutePath}/bin"
    var runner = file("${binDir}/fray.template").readText()
    if (currentProject.hasProperty("fray.installDir")) {
      val installDir = currentProject.property("fray.installDir")
      runner = runner.replace("#JAVA_PATH#", "$installDir/java-inst/bin/java")
      runner = runner.replace("#JVM_TI_PATH#", "$installDir/native-libs/libjvmti.$soSuffix")
      runner = runner.replace("#AGENT_PATH#", "$installDir/libs/fray-instrumentation-agent-$currentVersion.jar")
      runner = runner.replace("#CORE_PATH#", "$installDir/libs/fray-core-$currentVersion.jar")
    } else {
      val instrumentationTask = evaluationDependsOn(":instrumentation:agent")
          .tasks.named("shadowJar").get()
      val instrumentation = instrumentationTask.outputs.files.first().absolutePath
      val core = tasks.named("shadowJar").get().outputs.files.first().absolutePath
      val jvmti = currentProject.project(":jvmti")
      val jdk = currentProject.project(":instrumentation:jdk")
      runner = runner.replace("#JAVA_PATH#", "${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
      runner = runner.replace("#JVM_TI_PATH#", "${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.$soSuffix")
      runner = runner.replace("#AGENT_PATH#", instrumentation)
      runner = runner.replace("#CORE_PATH#", core)
    }
    val file = File("${binDir}/fray")
    file.writeText(runner)
    file.setExecutable(true)
  }
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.runtimeClasspath)
  into("${layout.buildDirectory.get().asFile}/dependency")
}

tasks.jar {
  manifest {
    attributes(mapOf("Main-Class" to "org.pastalab.fray.core.MainKt"))
  }
}

tasks.named<ShadowJar>("shadowJar") {
  relocate("org.objectweb.asm", "org.pastalab.fray.instrumentation.agent.asm")
  manifest {
    attributes(
      mapOf(
        "Main-Class" to "org.pastalab.fray.core.MainKt",
        "Premain-Class" to "org.pastalab.fray.core.PreMainKt"
      )
    )
  }
}

tasks.register<Jar>("sourceJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

