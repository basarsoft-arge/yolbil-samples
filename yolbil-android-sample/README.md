# Android Development Environment

## SDK Installation

Access to the SDK is managed through a Gradle plugin. To use this plugin, you must obtain a username and password from us and add the following lines to your gradle.properties file:

```
mavenUser=your_username
mavenPassword=your_password
```

```
Gradle ve Agp versiyonunu güncelleyiniz. 
```

### 1. Plugin Configuration (in android/build.gradle)

project -> build.gradle

// add to root
```
apply plugin: 'com.basarsoft.gradle.basarArtifactory'
```

// add to existing buildscript->dependencies
```
buildscript {
    ...
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        ...
        classpath 'com.basarsoft.gradle:artifactory-all:4.3.1'
    }
}
...
```
app -> build.gradle
```
dependencies {
    ...
    implementation 'com.basarsoft.yolbil:yolbil-mobile-sdk:0.0.94'
    
    implementation("us.inavi.libs:licensemanager:4.0.7")
    implementation("us.inavi.libs:sensormanager:4.0.7")
    
}
```
