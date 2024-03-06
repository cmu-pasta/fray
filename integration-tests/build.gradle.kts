plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(project(":core"))
}

tasks.test {
    useJUnitPlatform()

    val jvmti = project(":jvmti")
    val jdk = project(":jdk")
    val instrumentation = project(":instrumentation")
    executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
    jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so")
    jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
}