import java.util.Base64


//val ossrhUsernameProp = providers.gradleProperty("OSSRHUsername")
//
//
//val ossrhPasswordProp = providers.gradleProperty("OSSRHPassword")
//
//
//
//logger.lifecycle("OSSRH Username provided: ${ossrhUsernameProp.get()}")
//logger.lifecycle("OSSRH Password provided: ${!ossrhPasswordProp.orNull.isNullOrBlank()}")


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
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
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

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//            groupId = "io.github.sparrow007"
//            artifactId = "skyhigh-16kb-doctor"
//            version = "1.0.0-SNAPSHOT"
//
//            pom {
//                name.set("SkyHigh 16KB Page Alignment Doctor")
//                description.set("Gradle plugin to scan Android apps for 16KB page alignment compatibility")
//                url.set("https://github.com/sparrow007/skyhigh-16kb-doctor")
//
//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//
//                developers {
//                    developer {
//                        id.set("sparrow007")
//                        email.set("ankit.steven007@gmail.com")
//                    }
//                }
//
//                scm {
//                    connection.set("scm:git:git://github.com/sparrow007/skyhigh-16kb-doctor.git")
//                    developerConnection.set("scm:git:ssh://github.com:sparrow007/skyhigh-16kb-doctor.git")
//                    url.set("https://github.com/sparrow007/skyhigh-16kb-doctor")
//                }
//            }
//        }
//
//        repositories {
//            maven {
//                name = "OSSRH"
//                url = uri("https://central.sonatype.com/api/v1/publisher/upload")
//                credentials {
//                    username = ossrhUsernameProp.get()
//                    password = ossrhPasswordProp.get()
//                }
//            }
//        }
//    }
//}
//
//
//signing {
//    val key = providers.gradleProperty("OSSRHKeyAnkit")
//    val password = providers.gradleProperty("OSSRHPasswordAA")
//    logger.lifecycle("Signing key provided: ${!password.get().isNullOrBlank()}, password provided: ${password.get()}")
//    logger.lifecycle("Signing key provided: ${!key.get().isNullOrBlank()}, password provided: ${key.get()}")
//    //print the aboves
//
//
//    if (key.get().isNotBlank() && password.get().isNotBlank()) {
//        val decodedKey = String(Base64.getDecoder().decode(key.get()))
//        useInMemoryPgpKeys(decodedKey, password.get())
//        sign(publishing.publications)
//        println("✅ Signing enabled for publications")
//    } else {
//        println("⚠️ Signing disabled: missing signing.key or signing.password")
//    }
//}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

//tasks.named("publishMavenPublicationToMavenLocal") {
//    dependsOn(tasks.named("signPluginMavenPublication"))
//}
// Maven Publish from another Library
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates("io.github.sparrow007", "skyhigh-16kb-doctor", "1.0.0")

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


