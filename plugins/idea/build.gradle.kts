import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij.platform") version "2.10.4"
  id("org.jetbrains.changelog") version "2.2.1"
}

group = project.property("pluginGroup")!!

// Configure project's dependencies
repositories {
  mavenCentral()
  // IntelliJ Platform Gradle Plugin Repositories Extension - read more:
  // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

// Dependencies are managed with Gradle version catalog - read more:
// https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
  implementation(project(":rmi"))
  implementation(project(":mcp"))

  // IntelliJ Platform Gradle Plugin Dependencies Extension - read more:
  // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
  intellijPlatform {
    create(
        project.property("platformType")!! as String,
        project.property("platformVersion")!! as String)

    // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file
    // for bundled IntelliJ Platform plugins.
    bundledPlugins((project.property("platformBundledPlugins")!! as String).split(','))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for
    // plugin from JetBrains Marketplace.
    plugins((project.property("platformPlugins")!! as String).split(','))

    pluginVerifier()
    zipSigner()
    testFramework(TestFrameworkType.Platform)
    val jetbrainsRuntime = System.getenv("JETBRAINS_JDK_HOME")
    if (jetbrainsRuntime != null) {
      jetbrainsRuntimeLocal(jetbrainsRuntime)
    } else {
      jetbrainsRuntime()
    }
  }
}

// Configure IntelliJ Platform Gradle Plugin - read more:
// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
  buildSearchableOptions.set(false)
  pluginConfiguration {
    version = project.property("version")!! as String

    // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's
    // manifest
    description =
        providers.fileContents(rootProject.layout.projectDirectory.file("docs/IDE.md")).asText.map {
          val start = "<!-- Plugin description -->"
          val end = "<!-- Plugin description end -->"

          with(it.lines()) {
            if (!containsAll(listOf(start, end))) {
              throw GradleException(
                  "Plugin description section not found in README.md:\n$start ... $end")
            }
            subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
          }
        }
    changeNotes =
        "The change notes are available in the [CHANGELOG.md](https://github.com/cmu-pasta/fray/blob/main/CHANGELOG.md)"

    ideaVersion {
      sinceBuild = project.property("pluginSinceBuild")!! as String
      untilBuild = project.property("pluginUntilBuild")!! as String
    }
  }

  signing {
    certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PRIVATE_KEY")
    password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = providers.gradleProperty("intellijPlatformPublishingToken")
    // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release
    // labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically.
    // Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    channels =
        providers.gradleProperty("version").map {
          listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
  }

  pluginVerification { ides { recommended() } }
}

intellijPlatformTesting {
  runIde {
    register("runIdeForUiTests") {
      task(
          Action {
            jvmArgumentProviders += CommandLineArgumentProvider {
              listOf(
                  "-Drobot-server.port=8082",
                  "-Dide.mac.message.dialogs.as.sheets=false",
                  "-Djb.privacy.policy.text=<!--999.999-->",
                  "-Djb.consents.confirmation.enabled=false",
              )
            }
          })

      plugins { robotServerPlugin() }
    }
  }
}
