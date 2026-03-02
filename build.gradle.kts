plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "com.plicated"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.0.0"

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.netty:netty-codec-http:4.1.129.Final")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.plicated.MainKt")
}

configurations.all {
    resolutionStrategy.force("io.netty:netty-codec-http:4.1.129.Final")
}

tasks.test {
    useJUnitPlatform()
}
