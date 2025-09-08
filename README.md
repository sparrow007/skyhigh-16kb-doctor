# SkyHigh 16KB Doctor

A Gradle plugin that scans Android APK/AAB outputs for native `.so` libraries, checks ELF `p_align` values (16KB target), maps libraries back to owners (module or dependency), and produces machine- and human-friendly reports.

<img width="1472" height="704" alt="Image" src="https://github.com/user-attachments/assets/67afd550-931f-4ea1-8478-8b9c2abce572" />


## 🚀 Features

- **Automatic scanning** for 16KB page alignment
- **Easy integration** with Gradle
- **Detailed reports** for developers
- **Kotlin & Java support**

---

## 📦 Installation

Add the plugin to your `build.gradle.kts`:

In your Android application module (or app):

```kotlin
plugins {
  id("com.sparrow.skyhigh.16kb-doctor") version "1.0.0"
}
```

<hr></hr>

## 🛠 Usage

After applying the plugin, run:

```
./gradlew skyhighDoctorPlugin

```

## HTML Report

<img width="1884" height="278" alt="Image" src="https://github.com/user-attachments/assets/0558b9ca-7617-44f2-8e2e-10cf6408f69c" />
