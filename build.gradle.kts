import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage

plugins {
  kotlin("jvm") version "2.2.21"
  id("maven-publish")
  id("com.ncorti.ktfmt.gradle") version "0.25.0"
  id("org.jreleaser") version "1.16.0"
  id("com.gradleup.shadow") version "9.0.0-rc2"
  id("org.jetbrains.dokka") version "2.1.0"
  id("org.jetbrains.dokka-javadoc") version "2.1.0"
}

dokka {
  moduleName.set("Fray")
  dokkaSourceSets.main {
    includes.from("README.md")
    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      remoteUrl("https://github.com/cmu-pasta/fray")
      remoteLineSuffix.set("#L")
    }
  }
  pluginsConfiguration.html { footerMessage.set("(c) PASTA Lab, Carnegie Mellon University") }
}

tasks { wrapper { gradleVersion = "9.0.0" } }

allprojects {
  plugins.apply("com.ncorti.ktfmt.gradle")
  plugins.apply("base")
  base.archivesName =
      "${rootProject.name}-" + project.path.replaceFirst("^:".toRegex(), "").replace(':', '-')
  plugins.withId("org.jetbrains.kotlin.jvm") { kotlin { jvmToolchain(11) } }
  plugins.withType<JavaPlugin> {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }
  }
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
        create(
            "sonatype",
            Action {
              snapshotSupported = true
              stage = Stage.FULL
              active = Active.ALWAYS
              url = "https://central.sonatype.com/api/v1/publisher"
              stagingRepository("build/staging-deploy")
            })
      }
    }
  }
}

configure(
    allprojects.filter {
      it != rootProject &&
          it.path !in
              listOf(
                  ":instrumentation",
                  ":plugins",
                  ":plugins:gradle",
                  ":plugins:idea",
                  ":integration-test")
    }) {
      plugins.apply("maven-publish")
      plugins.apply("org.jetbrains.dokka")
      plugins.apply("org.jetbrains.dokka-javadoc")

      afterEvaluate {
        val dokkaJavadocJar by
            tasks.registering(Jar::class) {
              description = "A Javadoc JAR containing Dokka Javadoc"
              from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
              archiveClassifier.set("javadoc")
            }
        java { withSourcesJar() }
        publishing {
          publications {
            create<MavenPublication>("maven") {
              val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
              val arch = DefaultNativePlatform.getCurrentArchitecture().name.replace("-", "")
              pom {
                name = "Fray Testing Framework"
                description = "Fray testing framework for concurrent programs."
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
              if (components.findByName("shadow") == null || project.name == "core")
                  from(components["java"])
              else {
                from(components["shadow"])
                artifact(tasks["sourceJar"])
              }
              if (project.name != "jvmti") {
                artifact(dokkaJavadocJar)
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
