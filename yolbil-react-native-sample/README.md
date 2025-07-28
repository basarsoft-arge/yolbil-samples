
# React Native Development Environment

This document provides step-by-step instructions on how to set up the development environment for a React Native project and integrate the required SDK.

## SDK Installation

Access to the SDK is managed through a Gradle plugin. To use this plugin, you must obtain a username and password from us and add the following lines to your gradle.properties file:

```
mavenUser=your_username
mavenPassword=your_password
```

### 1. Plugin Configuration

a. Apply the Plugin

project -> android/build.gradle

Add to root
```
apply plugin: 'com.basarsoft.gradle.basarArtifactory'
```

b. Add to existing buildscript->dependencies

```
buildscript {
    ...
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        ...
        classpath "com.basarsoft.gradle:artifactory-all:4.2.2
    }
}
...
```
### 2. SDK Dependencies (in app/build.gradle)

```
dependencies {
    ...
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation 'us.inavi.libs:sensormanager:4.0.1'
    implementation 'us.inavi.libs:licensemanager:4.0.1'
}

```

Add to existing android

```
packagingOptions{
        pickFirst("lib/**/*.so")
    }
    splits {
        abi {
            reset()
            universalApk false 
            include "armeabi-v7a", "x86", "arm64-v8a", "x86_64"
        }
    }
```

##React Native Project Setup

Make sure the following tools are installed on your system:

- Node.js(v14 ve üzeri)
- Yarn
- JAVA JDK 17
- Android Studio

---

##Setup Steps

### 1. Clone the Repository

```bash
git clone https://your-repo-url.git
cd project-directory
```

### 2. Install Dependencies

```bash
yarn install
```

### 3. Start the Metro Server

```bash
yarn start
```

### 4. Run the Android App

```bash
npx react-native run-android
```
