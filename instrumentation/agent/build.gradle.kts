import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow)
}

dependencies {
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  api(project(":instrumentation:base"))
  compileOnly(project(":runtime"))
}

tasks.build { dependsOn("shadowJar") }

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("org.objectweb.asm", "org.pastalab.fray.instrumentation.agent.asm")
  dependencies {
    exclude(project(":runtime"))
    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
    exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
    exclude(dependency("org.intellij.lang.annotations:annotations"))
    exclude(dependency("org.jetbrains:annotations:.*"))
  }
  manifest {
    attributes(mapOf("Premain-Class" to "org.pastalab.fray.instrumentation.agent.PreMainKt"))
  }
}

tasks.jar { enabled = false }

tasks.register<Jar>("sourceJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

tasks.test { useJUnitPlatform() }
