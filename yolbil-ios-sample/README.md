## iOS (CocoaPods) ile Framework Ekleme Kılavuzu

Bu doküman, iOS projelerinize CocoaPods kullanarak yeni framework eklemeyi ve sıfırdan oluşturduğunuz bir projeye Yolbil SDK ve ilgili bağımlılıkları entegre etmeyi adım adım anlatır.

### Önkoşullar
- **Xcode** yüklü olmalı.
- **CocoaPods** sisteminizde kurulu olmalı. Kurulu değilse aşağıdaki komutlardan biriyle kurulumu yapabilirsiniz:

```bash
sudo gem install cocoapods
# veya Homebrew kullanıyorsanız
brew install cocoapods
```

### Mevcut projeye yeni framework ekleme (CocoaPods)
1. Proje klasörünüze gidin (içinde `*.xcodeproj` dosyanız olmalı).
2. Projede **Podfile** yoksa oluşturun:
   ```bash
   pod init
   ```
3. `Podfile` dosyasını açın ve aşağıdaki ayarları ekleyin/güncelleyin:
   - Yolbil pod kaynağı
   - Hedef (target) bloğu
   - `use_frameworks!` ve platform sürümü
   - Eklemek istediğiniz pod(lar)

   Örnek bir `Podfile` içeriği:
   ```ruby
   source 'https://github.com/basarsoft-arge/basarsoft-pod-repo.git'

   target 'UygulamaAdı' do
     use_frameworks!
     platform :ios, '13.0'

     pod 'YolbilMobileSDK', '2.5.7'
     # İsteğe bağlı ek modüller:
     # pod 'INVSensorManager'
     # pod 'INVPackageManager'
     # pod 'INVHelper'
     # pod 'INVPositioner'
     # pod 'INVRouting'
     # pod 'INVManifest'
     # pod 'INVNotificationService'
   end
   ```
4. Podları yükleyin veya güncelleyin:
   ```bash
   pod install
   # veya belirli bir podu güncellemek için
   # pod update YolbilMobileSDK
   ```
5. Projeyi artık `.xcworkspace` ile açın:
   ```bash
   open YourApp.xcworkspace
   ```
6. Kodda kullanmak için import edin:
   - Swift: `import YolbilMobileSDK`
   - Objective‑C: `@import YolbilMobileSDK;`

> Not: Bu örnek, bu depodaki `yolbilTest` örneğine benzer yapıdadır ve aynı pod kaynağını kullanır.

### Yeni bir Xcode projesi oluşturup Yolbil SDK’yı ekleme
1. Xcode → File → New → Project → iOS App ile yeni bir proje oluşturun.
2. Terminalde proje klasörüne gidin ve `Podfile` oluşturun:
   ```bash
   cd /path/to/YourNewApp
   pod init
   ```
3. `Podfile` dosyasını açıp aşağıdaki gibi düzenleyin:
   ```ruby
   source 'https://github.com/basarsoft-arge/basarsoft-pod-repo.git'

   target 'YourNewApp' do
     use_frameworks!
     platform :ios, '13.0'

     pod 'YolbilMobileSDK', '2.5.7'
     # İhtiyaca göre ek podlar:
     # pod 'INVSensorManager'
     # pod 'INVPackageManager'
     # pod 'INVHelper'
   end
   ```
4. Podları yükleyin ve `.xcworkspace` dosyasını açın:
   ```bash
   pod install
   open YourNewApp.xcworkspace
   ```
5. Uygulama kodunuzda ilgili modülleri import ederek kullanın.

### Sürüm yönetimi ve güncelleme
- Belirli sürüme sabitleme: `pod 'YolbilMobileSDK', '2.5.7'`
- Minör güncellemeleri alacak şekilde: `pod 'YolbilMobileSDK', '~> 2.5'`
- Güncelleme komutu: `pod update YolbilMobileSDK`
- Mevcut sürümleri görmek: `pod outdated`

### Sık karşılaşılan durumlar
- "No such module ..." hatası alırsanız projeyi `.xcworkspace` ile açtığınızdan emin olun.
- "Specs satisfying the `PodName` dependency were not found" benzeri uyarılarda pod kaynağı URL’sini ve internet bağlantısını kontrol edin, gerekirse `pod repo update` deneyin.
- Derleme ayarlarınızda iOS hedef sürümünün `Podfile` ile uyumlu olduğundan emin olun (ör. `iOS 13.0`).

### Bu örnek depoda kullanılan Podfile’dan kısa alıntı
Aşağıdaki yapı `yolbilTest` hedefi için örnektir:

```ruby
source 'https://github.com/basarsoft-arge/basarsoft-pod-repo.git'

target 'yolbilTest' do
  use_frameworks!
  platform :ios, '13.0'

  pod 'YolbilMobileSDK', '2.5.7'
  pod 'INVSensorManager'
  pod 'INVPackageManager'
  pod 'INVHelper'
end
```

Farklı bir framework eklemek için ilgili `pod 'FrameworkAdı'` satırını hedef bloğuna eklemeniz ve `pod install` komutunu çalıştırmanız yeterlidir.


