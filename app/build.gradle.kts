plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.github.sparrow007.skyhigh.16kb-doctor") version "1.0.1"
}


skyhighDoctor {
    variant.set("debug")
    assemble.set(true)
    scanApk.set(true)
    scanBundle.set(false)
    maxAlign.set(16384L) // or any value you want
}

// Task to copy the SkyHigh Doctor report to assets
abstract class CopySkyHighReportTask : DefaultTask() {
    @get:InputFile
    @get:Optional
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val targetFile: RegularFileProperty

    @TaskAction
    fun copyReport() {
        val source = sourceFile.get().asFile
        val target = targetFile.get().asFile

        if (source.exists()) {
            // Ensure parent directory exists
            target.parentFile.mkdirs()

            // Copy the file
            source.copyTo(target, overwrite = true)
            println("✅ SkyHigh Doctor report copied to assets: ${target.absolutePath}")
        } else {
            println("❌ SkyHigh Doctor report not found at: ${source.absolutePath}")
            println("ℹ️  Run './gradlew :app:skyhighDoctor' first to generate the report")
        }
    }
}

tasks.register<CopySkyHighReportTask>("copySkyHighReport") {
    group = "SkyHigh 16KB Doctor"
    description = "Copy the generated SkyHigh Doctor report to app assets"

    sourceFile.set(layout.buildDirectory.file("skyhigh/reports/final/final.html"))
    targetFile.set(layout.projectDirectory.file("src/main/assets/skyhigh_report.html"))
}

// Make the copy task run after the report is generated
tasks.named("skyhighDoctor") {
    finalizedBy("copySkyHighReport")
}

android {
    namespace = "com.sparrow.skyhigh_16kb_doctor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sparrow.skyhigh_16kb_doctor"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

// Create a solid task that uses shell script for reliable execution
tasks.register<Exec>("runWithFreshReport") {
    group = "SkyHigh 16KB Doctor"
    description = "Build, generate fresh report, and run the app (reliable two-stage build)"

    commandLine("./run-with-fresh-report.sh")
    workingDir = project.rootDir

    doFirst {
        println("🚀 Starting reliable build process with fresh SkyHigh Doctor report...")
    }
}

// Keep the automatic report generation for regular builds
afterEvaluate {
    tasks.matching { it.name.startsWith("assemble") }.configureEach {
        finalizedBy("skyhighDoctor")
    }
}