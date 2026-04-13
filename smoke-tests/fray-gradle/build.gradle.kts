import org.pastalab.fray.gradle.tasks.FrayTestTask

plugins {
    id("java")
    id("org.pastalab.fray.gradle") version "0.8.5-SNAPSHOT"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.build {
  dependsOn("frayTest")
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<FrayTestTask>()
    .configureEach {
  jvmArgs("-Xmx32G")
  jvmArgs("-Dfray.debug=true")
  testLogging {
    showStandardStreams = true
  }
}
