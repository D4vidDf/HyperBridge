<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="150" alt="HyperBridge Logo" style="border-radius: 20%;" />
</p>

<h1 align="center">Hyper Bridge</h1>

<p align="center">
  <strong>Bring the native HyperIsland experience to third-party apps on HyperOS.</strong>
</p>

<p align="center">
  Hyper Bridge bridges standard Android notifications into the pill-shaped UI around the camera cutout, offering a seamless, iOS-like experience on Xiaomi phones. Now with full theme customization and widget support.
</p>

<p align="center">
  <a href='https://play.google.com/store/apps/details?id=com.d4viddf.hyperbridge'>
    <img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height="80"/>
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-0.4.0-blue?style=for-the-badge&logo=github" alt="Version 0.4.0" />
  <img src="https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white" alt="Material Design" />
  <a href="https://crowdin.com/project/hyper-bridge"><img src="https://badges.crowdin.net/hyper-bridge/localized.svg" alt="Crowdin" /></a>
</p>

<br>

## 🚀 Features

* **Native Visuals:** Transforms notifications into HyperOS system-style islands.
* **🎨 Theme Engine (New):** Customize every pixel.
    * **Theme Creator:** Built-in editor to design your own themes with real-time previews.
    * **Smart Colors:** Automatically extract vibrant brand colors from app icons.
    * **Icon Shaping:** Choose between shapes like *Squircle*, *Clover*, *Arch*, and *Cookie*.
    * **Granular Control:** Per-app overrides for colors, icons, and action styles.
* **🧩 Widgets (New):** Pin standard Android widgets to the island layer for quick access—even on the Lockscreen!
* **Smart Integration:**
    * **🎵 Media:** Show album art and "Now Playing" status with visualizer support.
    * **🧭 Navigation:** Real-time turn-by-turn instructions (Google Maps, Waze).
    * **⬇️ Downloads:** Circular progress ring with a satisfying "Green Tick" animation upon completion.
    * **📞 Calls:** Dedicated layout for incoming and active calls with timers.
* **🛡️ Spoiler Protection:** Define blocked terms globally or per-app to prevent specific notifications (e.g., message spoilers) from popping up on the Island.
* **Total Control:** Choose exactly which apps trigger the island, customize timeouts, and toggle floating behavior per app.

## 👩‍💻 For Developers: Create Themes

HyperBridge supports an open theming standard (`.hbr` packages). You can create themes and distribute them, or integrate a "Apply Theme" button directly into your own app (Launcher, Icon Pack, etc.).

* **Documentation:** [Full Guide on Creating & Distributing Themes](https://github.com/D4vidDf/HyperBridge/discussions/78)
* **Intent API:** Send themes programmatically using `com.d4viddf.hyperbridge.APPLY_THEME`.

## 🌐 Supported Languages

HyperBridge is fully localized thanks to our amazing community. **Want to add your language?** We now use Crowdin for easy translation management.

👉 **[Help translate HyperBridge on Crowdin](https://crowdin.com/project/hyper-bridge)**

* 🇺🇸 **English** (Default)
* 🇪🇸 **Spanish** (Español)
* 🇧🇷 **Portuguese** (Português Brasileiro) — Thanks to [@NIICKTCHUNS](https://github.com/NIICKTCHUNS)
* 🇵🇱 **Polish** (Polski) — Thanks to [@kacskrz](https://github.com/kacskrz)
* 🇸🇰 **Slovak** (Slovenčina)
* 🇰🇷 **Korean** (한국어) — Thanks to [@alexkoala](https://github.com/alexkoala)
* 🇺🇦 **Ukrainian** (Українська) — Thanks to [@ItzDFPlayer](https://github.com/ItzDFPlayer)
* 🇷🇺 **Russian** (Русский) — Thanks to [@kilo3528](https://github.com/kilo3528)
* 🇩🇪 **German** (Deutsch) — Thanks to [@kilo3528](https://github.com/kilo3528)
* 🇮🇩 **Indonesian** (Bahasa Indonesia)
* 🇹🇷 **Turkish** (Türkçe)

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3 Expressive)
* **Architecture:** MVVM
* **Storage:** Room Database (SQLite)
* **Services:** NotificationListenerService, WidgetOverlayService
* **Concurrency:** Kotlin Coroutines & Flow

## 📸 Screenshots

| Home Screen | Active Island | Theme Creator | Widget Picker |
|:---:|:---:|:---:|:---:|
| ![Home](./screenshots/home.png) | ![Island](./screenshots/island_example.png) | ![Creator](./screenshots/theme_creator.png) | ![Widgets](./screenshots/widget_picker.png) |

## 📥 Installation

### Option 1: Google Play Store (Recommended)
The easiest way to install and keep the app updated.

<a href='https://play.google.com/store/apps/details?id=com.d4viddf.hyperbridge'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height="60"/></a>

### Option 2: Manual APK
1.  Download the latest APK from the [Releases](https://github.com/D4vidDf/HyperBridge/releases) page.
2.  Install the APK on your Xiaomi/POCO/Redmi device.

### ⚙️ Setup (Required for both methods)
1.  Grant **"Notification Access"** when prompted.
2.  **Critical:** Follow the in-app guide to enable **Autostart** and **No Restrictions** (Battery) to prevent the system from killing the background service.

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting a Pull Request.

1.  **Fork** the repository.
2.  Create a new branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a **Pull Request**.

## 💖 Support the Project

Hyper Bridge is an open-source project developed in my free time. If this app has improved your daily experience, please consider supporting its development!

<a href="https://github.com/sponsors/D4vidDf">
  <img src="https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86" width="150" alt="Sponsor"/>
</a>

## 📜 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.

## 👤 Developer

**D4vidDf**
* Website: [d4viddf.com](https://d4viddf.com)
* GitHub: [@D4vidDf](https://github.com/D4vidDf)