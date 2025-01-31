import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage

plugins {
  kotlin("jvm") version "2.0.0"
  id("maven-publish")
  id("com.ncorti.ktfmt.gradle") version "0.17.0"
  id("org.jetbrains.dokka") version "1.9.20"
  id("org.jreleaser") version "1.16.0"
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

allprojects {
  plugins.apply("com.ncorti.ktfmt.gradle")
  plugins.apply("base")
  base.archivesName = "${rootProject.name}-$name"
}

jreleaser {
  signing {
    active.set(Active.ALWAYS)
    armored.set(true)
  }
  deploy {
    maven {
      mavenCentral {
        active.set(Active.ALWAYS)
        create("sonatype") {
          snapshotSupported = true
          stage = Stage.UPLOAD
          active = Active.ALWAYS
          url = "https://central.sonatype.com/api/v1/publisher"
          stagingRepository("build/staging-deploy")
        }
      }
    }
  }
}

configure(allprojects - rootProject -
    project(":instrumentation") - project(":plugins").subprojects - project(":plugins")
    - project(":instrumentation:base") - project(":integration-test")) {
  plugins.apply("maven-publish")
  plugins.apply("org.jetbrains.dokka")
  afterEvaluate {
    tasks.register<Jar>("dokkaJavadocJar") {
      dependsOn(tasks.dokkaJavadoc)
      from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
      archiveClassifier.set("javadoc")
    }
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
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
              }
            }
            developers {
              developer {
                id = "aoli-al"
                name = "Ao Li"
                email = "aoli.al@hotmail.com"
              }
            }
            scm {
              connection = "scm:git:https://github.com/cmu-pasta/fray.git"
              developerConnection = "scm:git:ssh://github.com/cmu-pasta/fray.git"
              url = "https://github.com/cmu-pasta/fray"
            }
          }
          afterEvaluate {
            val shadowJar = tasks.findByName("shadowJar")
            if (shadowJar == null) from(components["java"])
            else artifact(shadowJar)
          }
          if (project.name != "jvmti") {
            artifact(tasks["dokkaJavadocJar"])
            artifactId = project.base.archivesName.get()
          } else {
            artifactId = project.base.archivesName.get() + "-$os-$arch"
          }
        }
      }
      repositories {
        maven {
          url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
      }
    }
  }
}
