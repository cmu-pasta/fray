import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
  id("java")
  kotlin("jvm")
  id("com.gradleup.shadow")
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  implementation(project(":instrumentation:base"))
  compileOnly(project(":runtime"))
}

tasks.build {
  dependsOn("shadowJar")
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("org.objectweb.asm", "org.pastalab.fray.instrumentation.agent.asm")
  dependencies {
    exclude(project(":runtime"))
  }
  manifest {
    attributes(mapOf("Premain-Class" to "org.pastalab.fray.instrumentation.agent.PreMainKt"))
  }
}

tasks.jar {
  enabled = false
}

tasks.register<Jar>("sourceJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

tasks.test {
  useJUnitPlatform()
}
