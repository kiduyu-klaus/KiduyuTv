<div align="center">

<!-- ═══════════════════════  LOGO / HERO  ═══════════════════════ -->

```
██╗  ██╗██╗██████╗ ██╗   ██╗██╗   ██╗    ████████╗██╗   ██╗
██║ ██╔╝██║██╔══██╗██║   ██║╚██╗ ██╔╝    ╚══██╔══╝██║   ██║
█████╔╝ ██║██║  ██║██║   ██║ ╚████╔╝        ██║   ██║   ██║
██╔═██╗ ██║██║  ██║██║   ██║  ╚██╔╝         ██║   ╚██╗ ██╔╝
██║  ██╗██║██████╔╝╚██████╔╝   ██║          ██║    ╚████╔╝
╚═╝  ╚═╝╚═╝╚═════╝  ╚═════╝   ╚═╝          ╚═╝     ╚═══╝
```

**Android TV & Fire TV streaming app — lean-back interface, built for the big screen.**

<!-- ═══════════════════════  BADGES ROW 1  ═══════════════════════ -->

<p>
  <!-- Build status -->
  <a href="../../actions/workflows/build-release.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/KiduyuKlaus/KiduyuTv/build-release.yml?branch=main&style=for-the-badge&logo=github-actions&logoColor=white&label=BUILD&color=22c55e&labelColor=0d1117" alt="Build Status"/>
  </a>
  <!-- Latest release -->
  <a href="../../releases/latest">
    <img src="https://img.shields.io/github/v/release/KiduyuKlaus/KiduyuTv?include_prereleases&style=for-the-badge&logo=android&logoColor=white&label=RELEASE&color=3b82f6&labelColor=0d1117" alt="Latest Release"/>
  </a>
  <!-- Download APK -->
  <a href="../../releases/latest">
    <img src="https://img.shields.io/github/downloads/KiduyuKlaus/KiduyuTv/total?style=for-the-badge&logo=docusign&logoColor=white&label=DOWNLOADS&color=8b5cf6&labelColor=0d1117" alt="Downloads"/>
  </a>
</p>

<p>
  <!-- Platform: Android TV -->
  <img src="https://img.shields.io/badge/Android%20TV-API%2026+-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117" alt="Android TV"/>
  <!-- Platform: Fire TV -->
  <img src="https://img.shields.io/badge/Fire%20TV-Compatible-FF9900?style=for-the-badge&logo=amazon&logoColor=white&labelColor=0d1117" alt="Fire TV"/>
  <!-- Language -->
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=0d1117" alt="Java 17"/>
</p>

<p>
  <!-- ExoPlayer -->
  <img src="https://img.shields.io/badge/ExoPlayer-1.9.0-00BCD4?style=for-the-badge&logo=youtube&logoColor=white&labelColor=0d1117" alt="ExoPlayer"/>
  <!-- TMDB -->
  <img src="https://img.shields.io/badge/TMDB-API-01D277?style=for-the-badge&logo=themoviedatabase&logoColor=white&labelColor=0d1117" alt="TMDB"/>
  <!-- License -->
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/KiduyuKlaus/KiduyuTv?style=for-the-badge&color=f59e0b&labelColor=0d1117&label=LICENSE" alt="License"/>
  </a>
</p>

---

</div>

## 📺 Overview

**KiduyuTv** is a native Android TV and Fire TV application that brings a curated catalog of movies and TV shows directly to your living room. Built with a true lean-back experience in mind — every interaction is optimised for D-pad remote navigation, large screens, and 10-foot UI principles.

| Feature | Details |
|---|---|
| 🎬 **Content** | Movies, TV shows, by Production Company & TV Network |
| 🔍 **Discovery** | Search, genre browsing, cast & crew detail pages |
| ▶️ **Playback** | HLS · DASH · Progressive via ExoPlayer with multi-server failover |
| 📝 **Subtitles** | External subtitle fetch via Subdl, SRT · VTT · SSA/ASS · TTML |
| 🎮 **Navigation** | Full D-pad focus management, scale animations, back-stack handling |
| 💾 **History** | Watch progress saved and resumed per title |
| ⚙️ **Settings** | Playback buffer duration, preferences persistence |

---

## 📸 Screens

| Home | Details | Player |
|:---:|:---:|:---:|
| Category rows with D-pad focus | TV show seasons & episode grid | Full-screen ExoPlayer controls |

---

## 🏗️ Architecture

```
app/src/main/java/com/kiduyu/klaus/kiduyutv/
├── Api/
│   ├── TmdbApi.java            — TMDB REST calls (movies, TV, search, cast)
│   ├── TmdbRepository.java     — Repository layer with async callbacks
│   ├── FetchStreams.java        — Multi-server stream fetcher (Hexa, Videasy…)
│   └── CastRepository.java     — Cast & crew lookup
├── Ui/
│   ├── splash/                 — Splash / entry point
│   ├── home/MainActivity.java  — Home screen, CategoryAdapter carousel rows
│   ├── search/                 — Search activity
│   ├── details/
│   │   ├── movie/              — Movie detail page
│   │   ├── tv/DetailsActivityTv— TV show detail + season tabs + episode grid
│   │   ├── actor/              — Actor filmography page
│   │   └── CompanyContentActivity — Production company / network catalog
│   ├── player/PlayerActivity   — ExoPlayer full-screen player
│   └── settings/               — App settings
├── adapter/
│   ├── CategoryAdapter.java    — Outer carousel (media rows + company rows)
│   ├── ContentCarouselAdapter  — Horizontal media card row
│   ├── CompanyNetworkAdapter   — Production company / TV network cards
│   ├── CompanyContentAdapter   — Grid of media by company/network
│   ├── EpisodeGridAdapter      — Episode grid (4 columns)
│   └── SeasonTabAdapter        — Season tab bar
├── model/
│   ├── MediaItems.java         — Core parcelable (movies, TV, video sources, subtitles)
│   ├── CategorySection.java    — Home row (TYPE_MEDIA or TYPE_COMPANY_NETWORK)
│   ├── CompanyNetwork.java     — Production company / TV network
│   ├── Episode.java            — Episode metadata
│   └── Season.java             — Season metadata
└── utils/
    ├── utils.java              — De-dupe sources, VIP reorder, helpers
    ├── PreferencesManager.java — Watch history, settings persistence
    └── SubdlService.java       — External subtitle fetch (Subdl API)
```

---

## 🛠️ Tech Stack

<!-- Tech badges as a visual grid -->
<div align="center">

| Layer | Library | Version |
|---|---|---|
| **Player** | AndroidX Media3 ExoPlayer | `1.9.0` |
| **Player — HLS** | media3-exoplayer-hls | `1.9.0` |
| **Player — DASH** | media3-exoplayer-dash | `1.9.0` |
| **Networking** | OkHttp | `4.12.0` |
| **REST** | Retrofit 2 + Gson | `2.11.0` / `2.10.1` |
| **Images** | Glide | `4.16.0` |
| **HTML parsing** | Jsoup | `1.21.2` |
| **Network layer** | Cronet (Chromium) | `66.3359.158` |
| **UI** | AppCompat · ConstraintLayout · RecyclerView · CardView | latest |
| **Lifecycle** | AndroidX Lifecycle (Java) | `2.7.0` |
| **Build** | Gradle `8.13` · AGP `8.13.2` · Java `17` | — |

</div>

---

## 🚀 Getting Started

### Prerequisites

- Android Studio **Ladybug** or newer
- JDK **17**
- A [TMDB API key](https://www.themoviedb.org/settings/api)

### Build & Run

```bash
# 1. Clone the repo
git clone https://github.com/KiduyuKlaus/KiduyuTv.git
cd KiduyuTv

# 2. Add your TMDB API key in:
#    app/src/main/java/com/kiduyu/klaus/kiduyutv/Api/ApiClient.java
#    → replace the BASE_URL / API_KEY constant

# 3. Build debug APK
./gradlew assembleDebug

# 4. Sideload to your Android TV / Fire TV device
adb connect <device-ip>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Sideload via ADB (no USB)

```bash
# Enable Developer Options on your TV, then:
adb connect 192.168.x.x:5555
adb install -r KiduyuTv-debug-*.apk

# Or push and install on device
adb push KiduyuTv-debug-*.apk /sdcard/Download/
# Then open a file manager app on the TV to install
```

---

## ⬇️ Download

Grab the latest debug APK directly from [**GitHub Releases →**](../../releases/latest)

Every push to `main` automatically builds and publishes a new debug APK via GitHub Actions.

---

## 🔄 CI / CD

```yaml
Trigger:  push to main  ·  pull_request  ·  workflow_dispatch
JDK:      17 (Temurin)
Gradle:   8.13
Output:   KiduyuTv-debug-<sha>.apk
Release:  auto-tagged  v{versionName}-build{commitCount}
```

The workflow at [`.github/workflows/build-release.yml`](.github/workflows/build-release.yml):

- Builds `assembleDebug` with Gradle cache for fast incremental builds
- Renames the APK with the short commit SHA for traceability
- Uploads it as a workflow artifact (30-day retention) on every run
- Creates or updates a pre-release on GitHub Releases on every `main` push
- Skips the release step on pull requests (artifact only)

---

## 📡 Supported Stream Formats

| Format | Support |
|---|---|
| HLS (`.m3u8`) | ✅ Full adaptive bitrate |
| DASH (`.mpd`) | ✅ Full adaptive bitrate |
| Progressive MP4 | ✅ Direct play |
| Multi-server failover | ✅ Auto-advances on error |
| Custom headers (Referer / Origin) | ✅ Per-source |

## 📝 Subtitle Support

| Format | MIME type |
|---|---|
| WebVTT `.vtt` | `text/vtt` |
| SubRip `.srt` | `application/x-subrip` |
| SSA / ASS `.ssa` `.ass` | `text/x-ssa` |
| TTML `.ttml` `.xml` | `application/ttml+xml` |

External subtitles are fetched on-demand from the **Subdl** API using the TMDB ID, season, and episode number.

---

## 🎮 Remote Navigation

KiduyuTv is built from the ground up for D-pad remotes:

- **D-pad Up/Down** — navigate between category rows; show player controls
- **D-pad Left/Right** — seek ±10 s when controls are hidden; scroll carousels when visible
- **D-pad Center / OK** — select item · play/pause
- **Back (single)** — hide player controls
- **Back (double)** — exit player
- **Fast Forward / Rewind media keys** — seek ±10 s

Focus animations (scale + elevation) are applied on every focusable card for clear visual feedback.

---

## 📂 Project Structure (key files)

```
KiduyuTv/
├── .github/
│   └── workflows/
│       └── build-release.yml       ← CI/CD pipeline
├── app/
│   ├── build.gradle                ← versionName, minSdk 26, compileSdk 36
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/…                  ← source (see Architecture above)
│       └── res/
│           ├── layout/             ← XML layouts
│           ├── drawable/           ← selectors, backgrounds
│           └── values/             ← colors, strings, themes
├── gradle/
│   ├── libs.versions.toml          ← version catalog
│   └── wrapper/
├── companies.json                  ← Production company seed data
├── networks.json                   ← TV network seed data
└── README.md
```

---

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Commit: `git commit -m "feat: add my feature"`
4. Push: `git push origin feat/my-feature`
5. Open a Pull Request — the CI will build a debug APK automatically

---

## 📄 License

Distributed under the terms of the [LICENSE](LICENSE) file in this repository.

---

<div align="center">

Built with ❤️ for the big screen · Powered by [TMDB](https://www.themoviedatabase.org)

<img src="https://img.shields.io/badge/Made%20for-Android%20TV-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Made for Android TV"/>
<img src="https://img.shields.io/badge/Powered%20by-ExoPlayer-00BCD4?style=flat-square&logo=youtube&logoColor=white" alt="ExoPlayer"/>
<img src="https://img.shields.io/badge/Data%20by-TMDB-01D277?style=flat-square&logo=themoviedatabase&logoColor=white" alt="TMDB"/>

</div>
