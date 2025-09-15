import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `kotlin-dsl`
    signing
    id("com.gradle.plugin-publish") version "1.2.1"
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.34.0"
}


repositories {
    mavenCentral()
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

// Kotlin toolchain + bytecode target 17
kotlin { jvmToolchain(17) }
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.24.2")
}

sourceSets {
    main {
        resources.srcDirs("src/main/resources")
        java.setSrcDirs(listOf("src/main/kotlin"))
    }
}


gradlePlugin {
    website.set("https://github.com/sparrow007/skyhigh-16kb-doctor")
    vcsUrl.set("https://github.com/sparrow007/skyhigh-16kb-doctor")

    plugins {
        create("skyhighDoctorPlugin") {
            id = "io.github.sparrow007.skyhigh.16kb-doctor"
            implementationClass = "com.sparrow.plugin.SkyHighDoctorPlugin"
            displayName = "SkyHigh 16KB Page Alignment Doctor"
            description = "Gradle plugin to scan Android apps for 16KB page alignment compatibility"
            tags.set(listOf("android", "kotlin", "plugin", "16kb", "page-alignment", "doctor"))
        }
    }
}


tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates("io.github.sparrow007", "skyhigh-16kb-doctor", "1.0.1")

    pom {
        name.set("SkyHigh 16KB Doctor")
        description.set("A Gradle plugin to scan Android apps for 16KB page alignment compatibility")
        inceptionYear.set("2025")
        url.set("https://github.com/sparrow007/skyhigh-16kb-doctor")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("sparrow007")
                email.set("ankit.steven007@gmail.com")
                name.set("Ankit")
                url.set("https://github.com/sparrow007/")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/sparrow007/skyhigh-16kb-doctor.git")
            developerConnection.set("scm:git:ssh://github.com:sparrow007/skyhigh-16kb-doctor.git")
            url.set("https://github.com/sparrow007/skyhigh-16kb-doctor")
        }
    }
}


