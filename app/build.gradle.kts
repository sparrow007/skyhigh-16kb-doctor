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


}