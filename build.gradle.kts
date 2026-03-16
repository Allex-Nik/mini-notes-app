plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.compose") version "1.10.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

group = "org.education"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.mysql:mysql-connector-j:9.5.0")
    implementation("org.hibernate:hibernate-core:7.2.2.Final")
    implementation("org.hibernate.validator:hibernate-validator:9.1.0.Final")
    implementation("org.glassfish.expressly:expressly:6.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources:1.10.2")
    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "org.education.MainKt"
    }
}