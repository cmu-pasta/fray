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

tasks.register<JavaExec>("runWith") {
    var jvmti = project(":jvmti")
    var jdk = project(":jdk")
    var instrumentation = project(":instrumentation")
    classpath = sourceSets["main"].runtimeClasspath
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    mainClass.set("cmu.pasta.sfuzz.core.MainKt")
    args = listOf("Harness", "-o", "${layout.buildDirectory.get().asFile}/report", "-a", "luindex")
    jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
}

