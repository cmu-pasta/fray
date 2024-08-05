
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.regex.Pattern
plugins {
  id("java")
  kotlin("jvm")
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.junit.vintage:junit-vintage-engine:5.10.2")
  implementation("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.test {
  useJUnitPlatform()
}

tasks.compileJava {
  options.compilerArgs.addAll(listOf("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED"))
}

tasks.named<ShadowJar>("shadowJar") {
  manifest {
    attributes(mapOf("Main-Class" to "org.pastalab.fray.core.MainKt"))
  }
}


tasks.named("build") {
  dependsOn("shadowJar")
}
