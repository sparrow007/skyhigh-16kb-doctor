# SkyHigh 16KB Doctor

A Gradle plugin that scans Android APK/AAB outputs for native `.so` libraries, checks ELF `p_align` values (16KB target), maps libraries back to owners (module or dependency), and produces machine- and human-friendly reports.


# Usage

In your Android application module (or sample-app):

1. Include the plugin (use `includeBuild` during development or publish and apply):
```kotlin
plugins {
  id("io.skyhigh.doctor")
}

skyhighDoctor {
  variant.set("release")
  assemble.set(true)
  scanApk.set(true)
  scanBundle.set(false)
  maxAlign.set(16384L)
  failOnViolation.set(false)
}
