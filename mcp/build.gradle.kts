plugins { kotlin("jvm") }

group = "org.pastalab.fray.mcp"

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  implementation(project(":rmi"))
  implementation("io.modelcontextprotocol:kotlin-sdk-server:0.7.7")
  implementation("io.ktor:ktor-server-cio:3.3.2")
  implementation("io.ktor:ktor-server-sse:3.3.2")
}

tasks.test { useJUnitPlatform() }
