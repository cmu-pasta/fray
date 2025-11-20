import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow)
}

dependencies {
  implementation(libs.commons.compress)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("org.apache.commons", "org.pastalab.fray.apache.commons")
}

val createVersionProperties by
    tasks.registering(WriteProperties::class) {
      val filePath =
          sourceSets.main.map {
            it.output.resourcesDir!!
                .resolve("org")
                .resolve("pastalab")
                .resolve("fray")
                .resolve("plugins")
                .resolve("base")
                .resolve("version.properties")
          }
      destinationFile = filePath
      property("version", project.version.toString())
    }

tasks.classes { dependsOn(createVersionProperties) }

tasks.jar { enabled = false }

tasks.register<Jar>("sourceJar") {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

tasks.test { useJUnitPlatform() }
