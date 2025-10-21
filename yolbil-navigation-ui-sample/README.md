# Basarsoft Yolbil Navigation UI

Bu döküman, örnek React Native uygulamasında Basarsoft Yolbil Navigation UI Android entegrasyonu için gerekli adımları özetler.

## 1) Basarsoft registry kaydı

Basarsoft kütüphanesine erişim için `~/.npmrc` dosyanıza aşağıdaki satırları ekleyin:

```
@basarsoft:registry=https://artifactory.basarsoft.com.tr/artifactory/generic-dev-local/npm
always-auth=true
```

Komut satırından eklemek için:

```bash
echo "@basarsoft:registry=https://artifactory.basarsoft.com.tr/artifactory/generic-dev-local/npm" >> ~/.npmrc

echo "always-auth=true" >> ~/.npmrc
```

## 2) Min SDK

Android minimum SDK versiyonu 24 veya üzeri olmalıdır.

```
minSdkVersion = 24
```

> Not: Projede minSdkVersion >= 24 olacak şekilde ayarlayın.

## 3) package.json bağımlılığı

`@basarsoft/react-native-yolbilnavigationui` bağımlılığını `package.json` içerisine ekleyin:

```json
"dependencies": {
    "@basarsoft/react-native-yolbilnavigationui": "https://artifactory.basarsoft.com.tr/artifactory/generic-dev-local/npm/dist/basarsoft-react-native-yolbilnavigationui-0.1.13.tgz"
}
```

Ardından kurulum:

- npm kullanıyorsanız:

```bash
npm install
```

- yarn kullanıyorsanız:

```bash
yarn install
```

## 4) Android Gradle ayarları

`android/build.gradle` dosyasında Basarsoft Artifactory eklentilerini tanımlayın:

```gradle
apply plugin: 'com.basarsoft.gradle.basarArtifactory'

buildscript {
    dependencies {
        classpath 'com.basarsoft.gradle:artifactory-all:4.3.1'
    }
}

```

`android/app/build.gradle` dosyasında Basarsoft Artifactory eklentilerini tanımlayın:

```
android {
  packagingOptions {
    pickFirst("lib/**/*.so")
 }
}
```


## 5) Artifactory erişim bilgileri

`android/gradle.properties` dosyasına kurumunuza/firmanıza tanımlanan Artifactory bilgilerini ekleyin:

```
mavenUser=<kullanici_adi>
mavenPassword=<kullanici_parola>
```

## 6) Tema ayarı (styles.xml)

`android/app/src/main/res/values/styles.xml` içindeki `AppTheme` tanımında `parent` değerini aşağıdaki gibi güncelleyin:

```xml
<style name="AppTheme" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
    <!-- Özelleştirmeleriniz -->
</style>
```

## 8) Kullanım (Örnek)

Örnek uygulamada kütüphane kullanımı aşağıdaki gibidir. Önce seçenekleri ayarlayıp ardından navigasyon ekranını açın:

```javascript
import { setNavigationOptions, openNavigationScreen } from '@basarsoft/react-native-yolbilnavigationui';

await setNavigationOptions({
  bmsAppCode: '...'
  , bmsAccountId: '...'
  , bmsServiceUrl: 'https://bms.basarsoft.com.tr/service'
  , mockGps: true
  , reCalculateRouteEnabled: true
  , useLiveTraffic: true
});

openNavigationScreen({ lat: 39.8902, lon: 32.82435 });
```

> Not: `bmsAppCode` ve `bmsAccountId` değerleriniz kurumunuza özel olmalıdır.


