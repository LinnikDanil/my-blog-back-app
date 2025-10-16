plugins {
    id("java")
}

group = "ru.yandex"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework:spring-core:6.2.11")
    implementation("org.springframework:spring-web:6.2.11")
    implementation("org.springframework.data:spring-data-jdbc:3.5.4")

    implementation("org.postgresql:postgresql:42.7.8")

    implementation("org.projectlombok:lombok:1.18.42")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework:spring-test:6.2.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testImplementation("com.h2database:h2:2.4.240")}

tasks.test {
    useJUnitPlatform()
}
