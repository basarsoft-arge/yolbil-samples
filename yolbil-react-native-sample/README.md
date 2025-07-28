
# React Native development environment

## SDK Kurulumunun Yapılması

SDK’e ulaşım Gradle’a eklenilen plugin vasıtasıyla olmaktadır. Bu plugin’i kullanabilmek için tarafımızdan bir kullanıcı adı ve şifre alarak gradle.properties dosyası içerisine şu şekilde tanımlamanız gerekmektedir.
```
mavenUser=kullanici_adiniz
mavenPassword=sifreniz
```

Gradle Plugin’i projenize tanımlamak için ise aşağıdaki dosyalara ilgili satırları eklemeniz gerekmektedir.

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
        classpath "com.basarsoft.gradle:artifactory-all:4.2.2
    }
}
...
```
app -> build.gradle
```
dependencies {
    ...
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation 'us.inavi.libs:sensormanager:4.0.1'
    implementation 'us.inavi.libs:licensemanager:4.0.1'
}
```
// add to existing android
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
//add to root
```
task copyDownloadableDepsToLibs(type: Copy) {
    from configurations.implementation
    into 'libs'
}
```

## React Native Projesini Ayağa Kaldırma

React Native projesinin kurulumu için,aşağıdaki araçların sisteminizde yüklü olması gerekmektedir:

- Node.js(v14 ve üzeri)
- Yarn
- JAVA JDK 17
- Android Studio

---

## Kurulum Adımları

### 1. Projeyi Klonlayın

```bash
git clone https://your-repo-url.git
cd proje-klasoru
```

### Bağımlılıkları Kurun

yarn install

yarn start

npx react-native run-android
