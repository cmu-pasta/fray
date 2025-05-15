plugins { kotlin("jvm") }

group = "org.pastalab.fray.mcp"

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  implementation(project(":rmi"))
  implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")
}

tasks.test { useJUnitPlatform() }
