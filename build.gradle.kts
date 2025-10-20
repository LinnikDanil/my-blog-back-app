plugins {
    id("java")
    war
}

group = "ru.yandex"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation(platform("org.springframework:spring-framework-bom:6.2.11"))
    implementation("org.springframework:spring-webmvc")
//    implementation("org.springframework:spring-jdbc")

    // Servlet API
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // Bean Validation
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")

    // Data base
//    implementation("org.postgresql:postgresql:42.7.8")

    // Lombok
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("org.projectlombok:lombok:1.18.42")

    // Test
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.springframework:spring-test")
//    testImplementation("com.h2database:h2:2.4.240")
}

tasks.test {
    useJUnitPlatform()
}
