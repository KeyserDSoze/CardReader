# Stable Android signing

CardReader has a dedicated update identity:

- namespace / application ID: `com.keyserdsoze.cardreader`
- keystore alias: `cardreader`
- key: RSA 4096, valid until 2054
- release SHA-1: `39:68:F1:33:E2:AD:27:74:79:F3:FF:5C:95:54:B6:B1:F0:4C:C3:E1`
- release SHA-256: `10:16:DB:28:EA:CC:97:FB:5F:F3:17:D2:A1:FB:AF:40:A8:6B:F5:22:CA:C8:2E:C1:FB:E9:18:4D:04:08:72:49`

Never replace this key after publishing `0.0.1`; Android updates must be signed by the same identity.

## Repository secrets

- `CARDREADER_KEYSTORE_B64`: Base64 of the complete JKS.
- `CARDREADER_KEYSTORE_PASSWORD`: password for the JKS and `cardreader` entry.
- `CARDREADER_GOOGLE_WEB_CLIENT_ID`: OAuth Web Client ID used by Credential Manager. It can remain empty only in builds where Google sync is intentionally unavailable.

The workflow restores the JKS only under the ephemeral runner temp directory and calls Gradle with `-PrequireStableSigning=true`. A release therefore fails rather than silently changing signing identity.

The JKS, its Base64 representation and its password must never be committed, printed in CI logs, attached to a release or used as the sole recoverable backup. Keep an offline/recoverable copy in a password manager or other access-controlled backup.

## Google OAuth

Create an Android OAuth client for package `com.keyserdsoze.cardreader` with the release SHA-1 above and enable the Google Drive API. Credential Manager additionally needs an OAuth Web Client ID; store that ID in the GitHub secret named above.
