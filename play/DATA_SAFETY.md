# Google Play data-safety notes

Current 0.0.1 design:

- no advertising;
- no analytics;
- camera frames processed on-device and not retained;
- wallet data stored locally;
- optional user-initiated Google Drive `appDataFolder` sync;
- Google account identity stored locally only to maintain the selected connection;
- no developer-operated backend.

Review the final Play Console questionnaire against the exact production build and Google's then-current definitions before publishing.
