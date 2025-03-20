plugins {
  kotlin("jvm")
}

group = "org.pastalab.fray.mcp"

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":core"))
  implementation(project(":rmi"))
  implementation("io.modelcontextprotocol:kotlin-sdk:0.3.0")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
  useJUnitPlatform()
}
