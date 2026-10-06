# CardReader Privacy Policy

Effective: 6 October 2026

CardReader is designed to work without an account, advertising or analytics.

## Local data

Card names, barcode/QR values, notes, colors and favorites are stored in the app's private storage on the device. Camera frames are processed on-device to recognize codes and are not saved by CardReader.

## Google Drive sync

Google Drive sync is optional and starts only after the user explicitly connects a Google account and requests sync. CardReader requests the `drive.appdata` scope and stores its sync document in Google Drive's private application-data area. The app does not read or write the user's ordinary Drive files.

CardReader stores the selected account's identifier, email and display name locally so it can show which account is connected. OAuth access tokens are not persisted by CardReader.

## Advertising, analytics and sale of data

CardReader contains no advertising SDK, analytics SDK or user-tracking SDK. The developer does not sell personal data collected by CardReader.

## Permissions

- Camera: used only to scan supported codes.
- Internet: used only for the optional Google account/Drive sync path.

Disconnecting Google does not delete the local wallet. Deleting the app may remove its local private data according to Android backup/restore behavior.
