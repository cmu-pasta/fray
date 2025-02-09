import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  id("com.gradleup.shadow")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.commons:commons-compress:1.27.1")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("org.apache.commons", "org.pastalab.fray.apache.commons")
}

val createVersionProperties by tasks.registering(WriteProperties::class) {
  val filePath = sourceSets.main.map {
    it.output.resourcesDir!!.resolve("org/pastalab/fray/plugins/base/version.properties")
  }
  destinationFile = filePath
  property("version", project.version.toString())
}

tasks.classes {
  dependsOn(createVersionProperties)
}

tasks.jar {
  enabled = false
}


tasks.test {
  useJUnitPlatform()
}
