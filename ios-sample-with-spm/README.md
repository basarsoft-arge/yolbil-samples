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

## 📦 Swift Package Manager (SPM) ile Manuel Kurulum

Eğer sıfırdan yeni bir projeye Yolbil SDK'yı eklemek istiyorsanız:

### 1. Xcode'da Package Ekleyin
1. Xcode'da projenizi açın
2. **File → Add Package Dependencies** menüsüne gidin
3. Arama kutusuna şu URL'yi girin:
   ```
   https://github.com/basarsoft-arge/basarsoft-pod-repo.git
   ```
4. **Add Package** butonuna tıklayın
5. Version seçimini yapın (örn: `2.5.2-spm` veya `Up to Next Major Version`)
6. Hedef target'ınızı seçin ve **Add Package** ile tamamlayın

### 2. Kimlik Doğrulama (Gerekirse)

Eğer private repository erişimi gerekiyorsa, `~/.netrc` dosyasını oluşturun:

```bash
# Dosyayı oluşturun veya düzenleyin
nano ~/.netrc
```

Aşağıdaki içeriği ekleyin (kendi kimlik bilgilerinizle değiştirin):

```
machine artifactory.basarsoft.com.tr
login YOUR_ARTIFACTORY_USERNAME
password YOUR_ARTIFACTORY_PASSWORD
```

Dosya izinlerini ayarlayın:
```bash
chmod 600 ~/.netrc
```

### 3. Kodda Kullanım

Swift dosyanızda import edin:

```swift
import YolbilMobileSDK

// Harita kullanımı
let mapView = YolbilMap()
```

### 4. Package Güncellemeleri

Package'ı güncellemek için:
1. **File → Packages → Update to Latest Package Versions**
2. Veya terminal'den:
   ```bash
   xcodebuild -resolvePackageDependencies
   ```

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

- **YolbilMobileSDK** - Ana SDK
  - Repository: [https://github.com/basarsoft-arge/basarsoft-pod-repo.git](https://github.com/basarsoft-arge/basarsoft-pod-repo.git)
  - Son sürüm: 2.5.2-spm

### Ek Modüller (İsteğe Bağlı)
Aynı repository üzerinden şu modüller de eklenebilir:
- INVSensorManager - Sensör yönetimi
- INVPackageManager - Paket yönetimi
- INVHelper - Yardımcı fonksiyonlar
- INVPositioner - Konum belirleme
- INVRouting - Rota hesaplama
- INVManifest - Manifest yönetimi
- INVNotificationService - Bildirim servisi

## 🆚 CocoaPods vs SPM

Bu proje **Swift Package Manager (SPM)** kullanır. CocoaPods kullanımı için `yolbil-ios-sample` projesine bakın.

| Özellik | SPM (Bu Proje) | CocoaPods |
|---------|----------------|-----------|
| **Xcode Entegrasyonu** | ✅ Native, doğrudan desteklenir | ⚠️ Ek kurulum gerekli |
| **Kurulum** | File → Add Package Dependencies | `pod install` komutu |
| **Bağımlılık Çözümü** | Otomatik | Manuel (`pod update`) |
| **Workspace** | ❌ Gerekli değil (`.xcodeproj`) | ✅ Gerekli (`.xcworkspace`) |
| **Performans** | ⚡ Daha hızlı | 🐢 Biraz daha yavaş |
| **Git Entegrasyonu** | ✅ Package.resolved ile | ⚠️ Podfile.lock ile |
| **Çakışma Çözümü** | Otomatik | Manuel müdahale gerekebilir |
| **Xcode Sürümü** | Xcode 11+ | Tüm versiyonlar |

### Hangi Yöntemi Seçmelisiniz?

**SPM'i tercih edin:**
- ✅ Yeni projeler için
- ✅ Native Xcode deneyimi istiyorsanız
- ✅ Daha hızlı build süreleri istiyorsanız
- ✅ Ek araç kurulumu istemiyorsanız

**CocoaPods'u tercih edin:**
- ✅ Eski projelerde zaten CocoaPods kullanılıyorsa
- ✅ SPM'de bulunmayan bağımlılıklara ihtiyacınız varsa
- ✅ Daha detaylı konfigürasyon kontrolü istiyorsanız

## 📄 Lisans

Bu örnek proje, Yolbil SDK kullanımını göstermek amacıyla hazırlanmıştır.

## 🤝 Destek

Sorularınız için:
- 📧 Email: [arge@basarsoft.com.tr](mailto:arge@basarsoft.com.tr)
- 📖 Dokümantasyon: [Basarsoft](https://basarsoft.com.tr)
- 🔗 SPM Repository: [basarsoft-pod-repo](https://github.com/basarsoft-arge/basarsoft-pod-repo)

## 📚 Ek Kaynaklar

- [Basarsoft Pod Repository](https://github.com/basarsoft-arge/basarsoft-pod-repo) - SDK ve tüm modüller
- [Swift Package Manager Dokümantasyonu](https://swift.org/package-manager/)
- [Xcode Package Dependencies Rehberi](https://developer.apple.com/documentation/xcode/adding-package-dependencies-to-your-app)