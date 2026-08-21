<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Panels icon" width="160">
</p>

<h1 align="center">Panels</h1>

<p align="center">A smart comic book reader for Android.</p>

Panels is a personal fork of the discontinued
[Seeneva Reader](https://github.com/Seeneva/seeneva-reader-android). It preserves
Seeneva's on-device speech-balloon detection, OCR, and text-to-speech features while
improving reading on modern phones, tablets, and foldables.

## Download

Panels is distributed exclusively through
[GitHub Releases](https://github.com/tommasodotNET/seeneva-reader-android/releases).
Download the latest universal APK and install it on your Android device.

Because Panels uses its own application ID (`app.panels.reader`), it installs separately
from the original Seeneva app.

## Features

- CBZ, CBR, CB7, CBT, and PDF support
- On-device speech-balloon detection and assisted reading
- Configurable bubble zoom and direct tap-to-zoom
- Landscape two-page spreads with LTR and RTL/manga reading order
- Folder-style collections with representative cover artwork
- ComicRack metadata support
- On-device OCR and text-to-speech
- No ads or personal-data collection

## Installing an APK

Allow APK installation from your browser or file manager, open the downloaded APK, and
confirm the installation. Existing Panels installations can be updated in place when the
APK is signed with the same release key.

With Android Debug Bridge:

```bash
adb install -r panels-1.0.0.apk
```

## Building

Development requirements and build instructions are documented in
[`docs/DEVELOPING.md`](docs/DEVELOPING.md).

```bash
./gradlew :app:assembleGithubDebug -Pseeneva.disableSplitApk
```

Version tags such as `v1.0.0` trigger the GitHub Actions release workflow, which builds
and publishes a signed universal APK.

## Upstream and attribution

Panels is based on Seeneva Reader, originally created by Sergei Solodovnikov. The native
reader is maintained for this fork at
[`tommasodotNET/seeneva-lib`](https://github.com/tommasodotNET/seeneva-lib).

## Privacy

Comic analysis runs locally on the device. See [`PRIVACY.md`](PRIVACY.md) for details.

## License

Panels remains licensed under the
[GNU General Public License v3.0 or later](https://www.gnu.org/licenses/gpl-3.0.html),
in accordance with the original project.

Third-party dependency notices are available in
[`logic/src/main/res/raw/dependencies.json`](logic/src/main/res/raw/dependencies.json)
and in the app's About screen.
