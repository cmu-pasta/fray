plugins {
  id("java")
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.2"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.jctools:jctools-core:3.1.0")
  testImplementation("com.googlecode.concurrent-trees:concurrent-trees:2.6.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
  implementation(project(":core"))
}

tasks.test {
  useJUnitPlatform()
  val agentPath: String by rootProject.extra
  val jdk = project(":jdk")
  val instrumentation = project(":instrumentation")
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  jvmArgs("-agentpath:$agentPath")
  jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.testRuntimeClasspath)
  into("${layout.buildDirectory.get().asFile}/dependency")
}

tasks.register<Jar>("testJar") {
  from(sourceSets.test.get().output)
}


