# Dynamic Island (Android)

Android için Dynamic Island benzeri bir kapsül: ekranın üstünde diğer uygulamaların
üzerinde durur, dokununca büyür.

## Yol haritası

| Aşama | İçerik | Durum |
|-------|--------|-------|
| 1 | Ada kapsülü, overlay izni, ön plan servisi, büyüme/küçülme animasyonu | ✅ |
| 2 | Bildirim dinleme ve bildirim paneli | ⬜ |
| 3 | Müzik kontrolü (medya oturumu, albüm kapağı, oynat/duraklat/ileri/geri) | ⬜ |
| 4 | Şarj animasyonları | ⬜ |
| 5 | Zamanlayıcı ve kronometre | ⬜ |
| 6 | Etkileşimli mini kontrol paneli | ⬜ |
| 7 | Özelleştirme: tema, animasyon, ayarlar ekranı | ⬜ |
| 8 | Cila: pil optimizasyonu, test, imzalı release APK | ⬜ |

## Derleme

**Android Studio:** klasörü aç, `app` yapılandırmasını çalıştır.

**Komut satırı:**

```bash
./gradlew assembleDebug
# çıktı: app/build/outputs/apk/debug/app-debug.apk
```

**GitHub Actions:** repoya push edince `Build APK` iş akışı çalışır; APK,
Actions sekmesinde işin altında `DynamicIsland-debug-apk` olarak indirilir.

## İlk çalıştırma

1. Uygulamayı aç, bildirim iznini ver (Android 13+).
2. **Overlay iznini ver** → sistem ayarlarında Dynamic Island için "Diğer uygulamaların üzerinde göster"i aç.
3. Geri dön, **Adayı başlat**.

Adaya dokununca büyür (4 sn sonra kendiliğinden küçülür). Uzun basınca uygulama açılır.

## Mimari

- `IslandView`: kapsülün kendisi. `collapsedContent` ve `expandedContent` alanlarına modüller view ekler.
- `IslandService`: ön plan servisi; adayı `WindowManager` ile `TYPE_APPLICATION_OVERLAY` olarak ekler.
- `MainActivity`: izin ve başlat/durdur ekranı.

Gereksinimler: minSdk 26, targetSdk/compileSdk 35, JDK 17.
