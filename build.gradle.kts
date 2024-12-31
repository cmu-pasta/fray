import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
  kotlin("jvm") version "2.0.0"
  id("maven-publish")
  id("com.ncorti.ktfmt.gradle") version "0.17.0"
  id("org.jetbrains.dokka") version "1.9.20"
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(21)
}

tasks {
  wrapper {
    gradleVersion = "8.10.2"
  }
}

configure(allprojects - project(":jvmti") - project(":instrumentation") - project(":plugins").subprojects) {
  plugins.apply("com.ncorti.ktfmt.gradle")
  afterEvaluate {
    tasks.register<Jar>("dokkaJavadocJar") {
      dependsOn(tasks.dokkaJavadoc)
      from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
      archiveClassifier.set("javadoc")
    }
  }
}

configure(allprojects - rootProject - project(":instrumentation") - project(":plugins").subprojects) {
  plugins.apply("maven-publish")
  plugins.apply("org.jetbrains.dokka")

  afterEvaluate {
    java {
      withSourcesJar()
    }
    publishing {
      publications {
        create<MavenPublication>("maven") {
          val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
          val arch = DefaultNativePlatform.getCurrentArchitecture().name
          pom {
            name = "Fray Testing Framework"
            description = "Fray testing framework for concurrency programs."
            url = "github.com/cmu-pasta/fray"
            licenses {
              license {
                name = "GPL-3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.html"
              }
            }
          }
          afterEvaluate {
            val shadowJar = tasks.findByName("shadowJar")
            if (shadowJar == null) from(components["java"])
            else artifact(shadowJar)
          }
          if (project.name != "jvmti") {
            artifact(tasks["dokkaJavadocJar"])
          } else {
            artifactId = "jvmti-$os-$arch"
          }
        }
      }
      repositories {
        maven {
          name = "GitHubPackages"
          url = uri("https://maven.pkg.github.com/cmu-pasta/fray")
          credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
          }
        }
      }
    }
  }
}
