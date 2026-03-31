plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.infinitybutterfly"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    // Cloudinary for image uploads
    implementation("com.cloudinary:cloudinary-http44:1.36.0")

    // JetBrains Exposed (Database ORM)
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")

    // PostgreSQL Driver (to connect to Neon)
    implementation("org.postgresql:postgresql:42.7.2")

    // HikariCP for database connection pooling (makes DB calls faster)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Dotenv to read hidden environment variables
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Password Hashing Library
    implementation("org.mindrot:jbcrypt:0.4")

    implementation("org.apache.commons:commons-email:1.5")
}
