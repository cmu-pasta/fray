import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage

plugins {
  alias(libs.plugins.kotlin.jvm)
  id("maven-publish")
  alias(libs.plugins.spotless)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.shadow)
  alias(libs.plugins.dokka)
  alias(libs.plugins.dokka.javadoc)
}

buildscript {
  dependencies {
    // We need this for idea plugin:
    // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2062
    classpath(libs.kotlinx.serialization.json)
  }
  configurations.classpath {
    resolutionStrategy {
      // Fix https://github.com/jreleaser/jreleaser/issues/1643 :(
      force("org.eclipse.jgit:org.eclipse.jgit:5.13.5.202508271544-r")
    }
  }
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
  plugins.apply("com.diffplug.spotless")
  plugins.apply("base")
  base.archivesName =
      "${rootProject.name}-" + project.path.replaceFirst("^:".toRegex(), "").replace(':', '-')
  plugins.withId("org.jetbrains.kotlin.jvm") { kotlin { jvmToolchain(11) } }
  plugins.withType<JavaPlugin> {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }
  }
  spotless {
    kotlin { ktfmt() }
    kotlinGradle {
      target("*.gradle.kts")
      ktfmt()
    }
    java {
      importOrder()
      removeUnusedImports()
      forbidWildcardImports()
      forbidModuleImports()
      cleanthat()
      googleJavaFormat()
    }
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
            },
        )
      }
    }
  }
}

configure(
    allprojects.filter {
      it != rootProject &&
          it.path !in
              listOf(
                  ":mcp",
                  ":instrumentation",
                  ":plugins",
                  ":plugins:gradle",
                  ":plugins:idea",
                  ":integration-test",
              )
    },
) {
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
          if (components.findByName("shadow") == null || project.name == "core") {
            from(components["java"])
          } else {
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
        maven { url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI() }
      }
    }
  }
}
