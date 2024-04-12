plugins {
    id("java")
}

group = "io.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.springframework:spring-jdbc:5.3.22")

    implementation ("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("com.h2database:h2:2.1.214")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation ("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
}
