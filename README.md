# SkyHigh 16KB Doctor

<img width="1472" height="704" alt="Image" src="https://github.com/user-attachments/assets/67afd550-931f-4ea1-8478-8b9c2abce572" />

[![Latest Version on Maven Central](https://img.shields.io/maven-central/v/io.github.sparrow007/skyhigh-16kb-doctor.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.sparrow007/skyhigh-16kb-doctor)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blueviolet?logo=kotlin&logoColor=white)
[![Build](https://github.com/sparrow007/skyhigh-16kb-doctor/actions/workflows/android-ci.yml/badge.svg)](https://github.com/sparrow007/skyhigh-16kb-doctor/actions/workflows/android-ci.yml)
![Gradle](https://img.shields.io/badge/Gradle-8.12.2-green?logo=gradle&logoColor=white)


## 📢 Project Description

This plugin is built with Gradle Kotlin DSL and written entirely in Kotlin, making it easy to integrate and maintain within modern Android projects.
It analyzes your Android project to identify native libraries (.so files) that are not compatible with the 16KB alignment requirement. The plugin then generates a clear report with:

- Incompatible libraries
- 16 KB Alignment
- Helps with large multi-module projects
- Cover all the CPU types (x86, x86_64, arm64-v8a, armeabi-v7a)
- Support JDK 17 For Running 

## ⚡ Why use it?

- Saves hours of manual investigation
- Works as an alternative to custom Lint checks
- Automates verification of native library alignment during the build process


## 📦 Installation

Add the plugin to your `build.gradle.kts`:

Make sure to add this in your Android application **entry module** (or app):

```kotlin
plugins {
    id("io.github.sparrow007.skyhigh.16kb-doctor") version "1.0.2"
}
```

### 🛠 In Settings.gradle 
**MaveCentral** should be included in plugin Management repo

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

## 🔁 Sync your project

After adding the dependency, click **"Sync Now"** in Android Studio or run the following command in your terminal to sync your project:


## 🛠 Usage

After applying the plugin, run: with your app **entry module** name 

```
./gradlew :app:skyhighDoctor

```


## 📝 HTML Report

<img width="1884" height="278" alt="Image" src="https://github.com/user-attachments/assets/0558b9ca-7617-44f2-8e2e-10cf6408f69c" />


### 🚀 Test dependencies fast using this project.

You can now directly test any new dependency in this project's **app module** without importing the full plugin into a large project.

Simply add the dependency you want to test in this project's app-level `build.gradle.kts`, then run:

```
./gradlew :app:runWithFreshReport
```

This command will build the app, generate a fresh report, and launch the app with the latest report.
<img width="3280" height="2048" alt="collage_landscape" src="https://github.com/user-attachments/assets/ef97d4b9-218b-4131-bd3e-dc183c90a2a3" />


<h2>📊 Report Column Explanation</h2>

<table>
  <thead>
    <tr>
      <th>Column</th>
      <th>Explanation</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><b>Artifact</b></td>
      <td>
        The build output where the check is performed:
        <ul>
          <li><b>Debug APK</b> → Built in debug mode</li>
          <li><b>Release APK</b> → Built in release mode</li>
        </ul>
        Helps identify which variant contains incompatible libraries.
      </td>
    </tr>
    <tr>
      <td><b>Path</b></td>
      <td>
        Shows the file path of the native library:
        <ul>
          <li><code>.so</code> files from dependencies</li>
          <li><code>jniLibs/</code> folder inside app/module</li>
        </ul>
        Lets developers trace the exact location of incompatible binaries.
      </td>
    </tr>
    <tr>
      <td><b>ABI</b></td>
      <td>
        The target CPU architecture for the library.  
        Common Android ABIs include:
        <ul>
          <li><code>armeabi-v7a</code></li>
          <li><code>arm64-v8a</code></li>
          <li><code>x86</code>, <code>x86_64</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><b>p_align</b></td>
      <td>
        Page alignment value of the native library.  
        Must be <code>16384 (16KB)</code> for compatibility with modern devices.  
        Any lower value (e.g., <code>4096</code>) is flagged ❌.
      </td>
    </tr>
    <tr>
      <td><b>16KB Compatible</b></td>
      <td>
        Indicates whether the library is compiled with <code>--page-size=16384</code>.
        <ul>
          <li>✅ Yes → Safe for 16KB page size</li>
          <li>❌ No → Must be updated/replaced</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td><b>Owner</b></td>
      <td>
        The dependency or module that <b>provides the .so file</b>.  
        This tells developers which library must be updated to fix incompatibility.
      </td>
    </tr>
  </tbody>
</table>


## 🤝 Contribution
We welcome contributions to the SkyHigh 16KB Doctor! Here are some ways you can help:

## 🐞 Report a Bug
If you find an issue, please open a new issue ticket on GitHub. Please provide as much detail as possible, including steps to reproduce the problem.


## 📝 Note:
This library is in a very early stage, which means there may be edge cases where it doesn’t work as expected. If you encounter any issues, please report them—we’ll review and address them in upcoming versions. Your feedback helps us improve the plugin.

## Find this repository useful? ❤️
Support it by joining __[stargazers](https://github.com/sparrow007/skyhigh-16kb-doctor/stargazers)__ for this repository. :star: <br>
 And __[follow](https://github.com/sparrow007)__  me for next creation 🤩

## License
```xml
Copyright 2025 Sparrow007 (Ankit)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
