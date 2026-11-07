# Dynamic Island (Android)

Android için Dynamic Island benzeri bir kapsül: ekranın üstünde diğer uygulamaların
üzerinde durur, dokununca büyür.

## Yol haritası

| Aşama | İçerik | Durum |
|-------|--------|-------|
| 1 | Ada kapsülü, overlay izni, ön plan servisi, büyüme/küçülme animasyonu | ✅ |
| 2 | Bildirim dinleme ve bildirim paneli | ✅ |
| 3 | Müzik kontrolü (medya oturumu, albüm kapağı, oynat/duraklat/ileri/geri) | ✅ |
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
3. **Bildirim erişimini ver** → listeden Dynamic Island'ı aç. Bu izin hem bildirimleri hem müzik oturumlarını okumak için gerekli.
   - Android 13+ üzerinde yandan yüklenen uygulamalarda seçenek gri olabilir: Ayarlar → Uygulamalar → Dynamic Island → ⋮ → **Kısıtlanmış ayarlara izin ver**, sonra tekrar dene.
4. Geri dön, **Adayı başlat**.

## Kullanım

- Adaya dokununca büyür (4 sn sonra kendiliğinden küçülür). Uzun basınca uygulama açılır.
- Büyükken **yana kaydırınca** sayfalar arasında gezilir: müzik → bildirimler → saat (olanlar).
- Yeni bildirim gelince ada 5 sn büyür ve bildirimi gösterir; satıra dokunmak bildirimi açar.
- Müzik çalarken ada müzik sayfasını gösterir: kapak, parça adı, ilerleme, önceki / oynat-duraklat / sonraki.
- Sessiz (düşük önemli) bildirimler listeye girer ama adayı büyütmez. Devam eden (ongoing), medya ve ön plan servisi bildirimleri bildirim listesine alınmaz.

## Mimari

- `IslandView`: kapsülün kendisi. `collapsedContent` ve `expandedContent` alanlarına modüller view ekler; yatay kaydırmayı algılar.
- `IslandController`: hangi modülün görüneceğine karar verir (öncelik, uyarı, kaydırma).
- `IslandModule`: sayfa arayüzü. Mevcut modüller: `ClockModule`, `NotificationModule`, `MediaModule`.
- `NotificationRepository` + `IslandNotificationListener`: bildirim akışı. `MediaMonitor`: medya oturumları.
- `IslandService`: ön plan servisi; adayı `WindowManager` ile `TYPE_APPLICATION_OVERLAY` olarak ekler ve modülleri bağlar.
- `MainActivity`: izin ve başlat/durdur ekranı.

Gereksinimler: minSdk 26, targetSdk/compileSdk 35, JDK 17.
