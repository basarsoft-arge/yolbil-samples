# iOS Sample with Swift Package Manager (SPM)

Bu proje, Yolbil SDK'yı Swift Package Manager (SPM) kullanarak iOS uygulamanıza nasıl entegre edeceğinizi gösteren örnek bir uygulamadır.

## 📋 Özellikler

- ✅ Swift Package Manager ile bağımlılık yönetimi
- ✅ Harita görüntüleme ve etkileşim
- ✅ Konum takibi
- ✅ Rota hesaplama ve çizimi
- ✅ Marker ekleme ve yönetimi
- ✅ Modern SwiftUI arayüzü
- ✅ MVVM mimarisi

## ⚠️ API Kimlik Bilgilerini Yapılandırma (ÖNEMLİ!)

Projeyi çalıştırmadan önce **mutlaka** API kimlik bilgilerinizi yapılandırmalısınız:

### 1. Secrets.swift Dosyasını Oluşturun
```bash
cd ios-sample-with-spm/ios-sample-with-spm/Utilities
cp Secrets.swift.template Secrets.swift
```

### 2. API Kimlik Bilgilerinizi Ekleyin
`Secrets.swift` dosyasını açın ve Yolbil dashboard'unuzdan aldığınız gerçek değerlerle güncelleyin:

```swift
enum Secrets {
    static let appCode = "YOUR_ACTUAL_APP_CODE"
    static let accountId = "YOUR_ACTUAL_ACCOUNT_ID"
}
```

> **🔒 Güvenlik Notu:** `Secrets.swift` dosyası `.gitignore`'a eklenmiştir ve **asla** commit edilmemelidir. Sadece `Secrets.swift.template` dosyası repository'de tutulur.

## 🚀 Kurulum

### Önkoşullar
- Xcode 15.0 veya üzeri
- iOS 15.0 veya üzeri
- Swift 5.9 veya üzeri

### Adımlar

1. **Projeyi klonlayın:**
   ```bash
   git clone https://github.com/basarsoft-arge/yolbil-samples.git
   cd yolbil-samples/ios-sample-with-spm
   ```

2. **API kimlik bilgilerini yapılandırın** (yukarıdaki adımları takip edin)

3. **Projeyi Xcode ile açın:**
   ```bash
   open ios-sample-with-spm.xcodeproj
   ```

4. **Swift Package bağımlılıklarını yükleyin:**
   - Xcode otomatik olarak Package.resolved dosyasını okuyacak ve gerekli paketleri indirecektir
   - Eğer otomatik olmazsa: File → Packages → Resolve Package Versions

5. **Projeyi derleyin ve çalıştırın:**
   - Bir simulator veya gerçek cihaz seçin
   - ⌘R ile çalıştırın

## 📱 Kullanım

### Temel Harita İşlemleri
- **Zoom:** + / - butonları veya pinch gesture
- **Pan:** Haritayı sürükleyin
- **Konum:** GPS butonuna tıklayarak konumunuzu görebilirsiniz

### Rota Oluşturma
1. Haritada bir nokta seçin (marker eklenir)
2. "Navigasyona Başla" butonuna tıklayın
3. Rota seçenekleri arasından birini seçin

### Marker Yönetimi
- Haritaya tıklayarak marker ekleyebilirsiniz
- Marker'lara tıklayarak detaylarını görebilirsiniz
- "Temizle" butonu ile tüm marker'ları silebilirsiniz

## 🏗️ Proje Yapısı

```
ios-sample-with-spm/
├── Models/              # Veri modelleri
│   ├── LocationData.swift
│   ├── MapMarker.swift
│   ├── RouteModels.swift
│   └── ...
├── ViewModels/          # MVVM view model'ler
│   └── MapViewModel.swift
├── Views/               # SwiftUI görünümleri
│   ├── Map/
│   └── Components/
├── Services/            # İş mantığı servisleri
│   ├── MapService.swift
│   ├── LocationService.swift
│   ├── RoutingService.swift
│   └── MarkerService.swift
├── Utilities/           # Yardımcı dosyalar
│   ├── Constants.swift
│   ├── Secrets.swift.template  # ⚠️ Bu dosyayı kopyalayın!
│   └── ModernMarkerIcon.swift
└── Assets/              # Stil paketleri ve kaynaklar
```

## 🔧 Yapılandırma

### API Endpoint'leri
`Constants.swift` dosyasında API endpoint'lerini yapılandırabilirsiniz:

```swift
enum Constants {
    enum API {
        static let host = "https://bms.basarsoft.com.tr"
        static let routingBasePath = "/service/api/v1/Routing/BasarRouting"
        // ... diğer endpoint'ler
    }
}
```

### Harita Ayarları
Harita görünümü ve zoom seviyeleri için:

```swift
enum Constants {
    enum Map {
        static let defaultZoom: Float = 10.0
        static let closeZoom: Float = 16.0
        static let trackingZoom: Float = 17.0
        // ... diğer ayarlar
    }
}
```

## 🎨 Stil Paketleri

Proje, harita görselleri için önceden yüklenmiş stil paketleri içerir:
- `transport_style_final_package_latest_light.zip` - Açık tema
- `transport_style_final_package_latest_dark.zip` - Koyu tema
- `CommandVoices.zip` - Sesli yönlendirme paketleri

## 🛠️ Sorun Giderme

### "No such module" hatası
- Xcode'u kapatıp tekrar açın
- File → Packages → Reset Package Caches
- Clean Build Folder (⌘⇧K)

### Harita görünmüyor
- API kimlik bilgilerinizi kontrol edin
- Internet bağlantınızı kontrol edin
- Console loglarını inceleyin

### Build hatası
- Minimum deployment target'in iOS 15.0 olduğundan emin olun
- Swift version'ın 5.9+ olduğundan emin olun

## 📚 Bağımlılıklar

Bu proje Swift Package Manager kullanır. Bağımlılıklar:
- YolbilMobileSDK (SPM üzerinden)

## 📄 Lisans

Bu örnek proje, Yolbil SDK kullanımını göstermek amacıyla hazırlanmıştır.

## 🤝 Destek

Sorularınız için:
- 📧 Email: [destek@basarsoft.com.tr](mailto:destek@basarsoft.com.tr)
- 📖 Dokümantasyon: [Yolbil Docs](https://docs.yolbil.com)

## 🆚 CocoaPods vs SPM

Bu proje SPM kullanır. CocoaPods kullanımı için `yolbil-ios-sample` projesine bakın.

| Özellik | SPM | CocoaPods |
|---------|-----|-----------|
| Xcode Entegrasyonu | Native | Ek kurulum gerekli |
| Bağımlılık Çözümü | Otomatik | `pod install` gerekli |
| Workspace | Gerekli değil | .xcworkspace gerekli |
| Hız | Daha hızlı | Biraz daha yavaş |

