# Android development environment

SDK Kurulumunun Yapılması
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
        classpath 'com.basarsoft.gradle:artifactory-all:4.2.1'
    }
}
...
```
app -> build.gradle
```
dependencies {
    ...
    implementation 'com.basarsoft.yolbil:yolbil-mobile-sdk:2.5.0'
}
```