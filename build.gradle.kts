plugins {
    id("java")
    war
}

group = "ru.yandex"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation(platform("org.springframework:spring-framework-bom:6.2.11"))
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.data:spring-data-jdbc:3.5.5")

    // Servlet API
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // Bean Validation
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")
    implementation("org.glassfish:jakarta.el:4.0.2")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")

    // Data base
    implementation("org.postgresql:postgresql:42.7.8")

    // Lombok
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("org.projectlombok:lombok:1.18.42")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.25.2")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.2")
    implementation("org.apache.logging.log4j:log4j-jcl:2.25.2")

    // Test
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.mockito:mockito-core:5.14.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.1")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
