# CardReader

Android wallet for loyalty cards and other cards represented by barcodes or 2D codes. Free, ad-free and offline-first.

## What 0.0.1 provides

- instant local wallet: opening and showing a card never requires internet;
- on-device scanning for QR, EAN-13/8, UPC-A/E, Code 128/39/93, Codabar, ITF, Data Matrix, Aztec and PDF417;
- bundled ML Kit barcode model, so the scanner has no first-use model download;
- offline rendering of saved codes with ZXing;
- favorites, search, notes, colors, manual add/edit/delete;
- maximum screen brightness while a card is open for reliable checkout scanning;
- optional, manual Google Drive sync in the private `appDataFolder`;
- per-card deterministic merge and deletion tombstones for multiple offline devices.

The project intentionally follows the layout and release conventions of `KeyserDSoze/Android.DiceThrower`: Android sources live under `src/`, semantic version lives in root `VERSION`, and GitHub Actions own CI and releases.

## Build

Requirements match DiceThrower: JDK 17, Android SDK 37 and Gradle 9.6.

```bash
cd src
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is `src/app/build/outputs/apk/debug/app-debug.apk`.

## Google Drive sync

Core app functionality does not initialize or contact Google. From Settings the user can opt in, authorize one Google account and press **Sync now**. Only the `drive.appdata` scope is requested.

Credential Manager needs an OAuth Web Client ID injected at build time as `CARDREADER_GOOGLE_WEB_CLIENT_ID`. The Android OAuth client for package `com.keyserdsoze.cardreader` must use the release SHA-1 documented in `docs/SIGNING.md`.

## Releases

`VERSION` starts at `0.0.1`. The release workflow builds a signed APK and AAB, verifies them, creates SHA-256 files and publishes all of them to a GitHub Release tagged `v<VERSION>`.

Required repository secrets:

- `CARDREADER_KEYSTORE_B64`
- `CARDREADER_KEYSTORE_PASSWORD`
- `CARDREADER_GOOGLE_WEB_CLIENT_ID`
- `CARDREADER_GOOGLE_ANDROID_CLIENT_ID` (release-readiness check for the package/certificate OAuth registration)

The release signing key itself is never committed. See `docs/SIGNING.md`.

## Privacy

No ads, analytics or tracking SDKs. See `PRIVACY.md`.

## License

MIT — see `LICENSE`.
