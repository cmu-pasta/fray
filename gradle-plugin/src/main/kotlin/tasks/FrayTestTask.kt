package cmu.pasta.fray.gradle.tasks

import cmu.pasta.fray.gradle.Commons
import java.io.File
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.Test

abstract class FrayTestTask : Test() {
  @get:Input abstract val frayInstrumentation: Property<Dependency>

  @Internal
  val javaPath =
      File("${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JAVA_PATH}")

  @Internal
  val jvmtiPath = "${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}"

  init {
    configFray()
  }

  fun configFray() {
    useJUnitPlatform { it.includeEngines("fray") }
    executable(javaPath.absolutePath)
    jvmArgs("-agentlib:$jvmtiPath/libjvmti.so")
    jvmArgs(
        "-javaagent:${project.configurations.detachedConfiguration(frayInstrumentation.get()).resolve().first()}")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
  }
}
