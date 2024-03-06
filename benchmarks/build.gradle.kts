import de.undercouch.gradle.tasks.download.Download
plugins {
    java
    id("de.undercouch.download") version "5.6.0"
}

val buildPath = layout.buildDirectory.get().asFile

dependencies {
    implementation(fileTree(mapOf("dir" to "${buildPath}/libs/unzipped", "include" to listOf("*.jar"))))
    implementation(project(":core"))
}


repositories {
    mavenCentral()
}


tasks.register<Download>("downloadDacapo") {
    src("https://download.dacapobench.org/chopin/dacapo-23.11-chopin.zip")
    dest(File(buildPath, "libs/dacapo.zip"))
    onlyIfModified(true)
}

tasks.register<Copy>("unzipDacapo") {
    dependsOn("downloadDacapo")
    from(zipTree("${buildPath}/libs/dacapo.zip"))
    into("${buildPath}/libs/unzipped")
}

tasks.withType<JavaExec> {
    val jvmti = project(":jvmti")
    val jdk = project(":jdk")
    val instrumentation = project(":instrumentation")
    classpath = sourceSets["main"].runtimeClasspath
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    mainClass = "cmu.pasta.sfuzz.core.MainKt"
    jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")

//    var appName = project.hasProperty('appName') ? project.property('appName') : "luindex"

    doFirst {
        // Printing the full command
        println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
    }
}


tasks.register<JavaExec>("run") {
    val appName = properties["appName"] as String? ?: "avrora"
    args = listOf("Harness", "-o", "${layout.buildDirectory.get().asFile}/report", "-a", appName, "--scheduler", "fifo", "-s", "true")
}

tasks.register<JavaExec>("replay") {
    val appName = properties["appName"] as String? ?: "avrora"
    args = listOf("Harness", "-o", "/tmp/report", "-a", appName, "--scheduler", "replay", "--path", "${layout.buildDirectory.get().asFile}/report/schedule_0.json", "-s", "true")
}

tasks.register<JavaExec>("debug") {
    val appName = properties["appName"] as String? ?: "avrora"
    args = listOf("Harness", "-o", "${layout.buildDirectory.get().asFile}/report", "-a", appName, "--scheduler", "fifo", "-s", "true")
    jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
}
