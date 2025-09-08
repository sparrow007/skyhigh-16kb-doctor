# SkyHigh 16KB Doctor

A Gradle plugin that scans Android APK/AAB outputs for native `.so` libraries, checks ELF `p_align` values (16KB target), maps libraries back to owners (module or dependency), and produces machine- and human-friendly reports.

<img width="1472" height="704" alt="Image" src="https://github.com/user-attachments/assets/67afd550-931f-4ea1-8478-8b9c2abce572" />

[![Latest Version on Maven Central](https://img.shields.io/maven-central/v/io.github.sparrow007/skyhigh-16kb-doctor.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.sparrow007/skyhigh-16kb-doctor)

## 🚀 Features

- **Automatic scanning** for 16KB page alignment
- **Easy integration** with Gradle
- **Detailed reports** for developers
- **Kotlin & Java support**


## 📦 Installation

Add the plugin to your `build.gradle.kts`:

In your Android application module (or app):

```kotlin
plugins {
  id("com.sparrow.skyhigh.16kb-doctor") version "1.0.1"
}
```

## 🛠 Usage

After applying the plugin, run:

```
./gradlew skyhighDoctorPlugin

```

## 🔁 Sync your project

After adding the dependency, click **"Sync Now"** in Android Studio or run the following command in your terminal to sync your project:

```sh
./gradlew build
```

## 📝 HTML Report

<img width="1884" height="278" alt="Image" src="https://github.com/user-attachments/assets/0558b9ca-7617-44f2-8e2e-10cf6408f69c" />

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


## Find this repository useful? ❤️
Support it by joining __[stargazers](https://github.com/sparrow007/skyhigh-16kb-doctor/stargazers)__ for this repository. :star: <br>
 And __[follow](https://github.com/sparrow007)__  me for next creation 🤩

## Thanks
Your feedback helps us improve the plugin.

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
