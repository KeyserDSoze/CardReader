# CardReader architecture

## Principles

- **Offline-first:** local cards are the source used by every normal UI action.
- **Fast startup:** no Google/Drive client is created on app startup.
- **Local ownership:** scans and rendering work on-device; network is only reachable from explicit sync.
- **Small surface:** one Android app module, versioned JSON persistence, no database startup cost for this data size.
- **Stable IDs:** cards use UUIDs, independent from their barcode value.

## Persistence

`LocalCardStore` keeps one schema-versioned `WalletDocument` in private SharedPreferences, mirroring the simple document-store boundary used by DiceThrower. Each card includes `updatedAt` and an installation `writerId`. The installation ID lives in `noBackupFilesDir` and is not part of the portable wallet.

Deleting a card creates a tombstone instead of forgetting its ID. Tombstones prevent a phone that was offline during a deletion from re-uploading the old card later.

If the model grows beyond small wallet scale, `LocalCardStore` is the boundary to replace with Room without changing the UI or Drive merge policy.

## Scanner and renderer

CameraX provides the preview and frame lifecycle. The bundled ML Kit barcode model analyzes frames fully on-device. CardReader does not use the Play-services dynamically downloaded barcode model.

Detected formats are mapped to the app's provider-neutral `CodeFormat`. ZXing Core renders the saved value back to the original supported symbology; rendering needs no camera, Google service or network connection.

## Google boundary

Google is opt-in from Settings. Credential Manager identifies the account. Google Play services `AuthorizationClient` separately requests only `https://www.googleapis.com/auth/drive.appdata`. Access tokens are requested on demand, used in the HTTPS Authorization header and never persisted.

Remote storage is one JSON document named `cardreader-wallet-v1.json` in Drive's hidden `appDataFolder`, tagged with CardReader-specific `appProperties`. The user-visible Drive root remains untouched.

## Multi-device merge

Sync reads local and remote documents and resolves every card ID independently. Higher `updatedAt` wins. Equal timestamps use stable writer/content tie-breakers so two devices converge on the same result. A deletion at the same version rank wins over a card value. Independent cards are unioned.

The merged result is committed locally, then uploaded back to the same logical remote document. Network and authorization failures never invalidate local data.

## Sync lifecycle

There is deliberately no automatic sync on cold start. The user presses **Sync now**. This keeps opening the app and presenting a checkout card independent of connectivity, DNS, Google Play services latency and Drive availability.
