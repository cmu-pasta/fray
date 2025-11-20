plugins { alias(libs.plugins.kotlin.jvm) }

dependencies { testImplementation(libs.kotlin.test) }

tasks.test { useJUnitPlatform() }
