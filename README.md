# PayBondhu Android app

Companion app for the PayBondhu auto payment gateway (paybondhu.com). It runs on the phone that receives your
bKash / Nagad / Rocket / Upay payment SMS and forwards those messages to your PayBondhu account so customer
payments can be verified automatically.

- Auto sync in the background with a permanent notification (shows that sync is on, last sync time and pending count)
- Offline queue with automatic retry, inbox scan for messages missed while the phone was off
- Full transaction history with search, filters, details and per-message status (Pending / Sent / Matched / Failed)
- English and Bangla

## How the app talks to the server

All calls are form-encoded `POST`s to `https://paybondhu.com/api/...`, authenticated with the account email and the
device key from **Dashboard → Integration**:

| Endpoint | Purpose |
| --- | --- |
| `api/device-connect` | Login, binds the key to this phone |
| `api/device-status` | Heartbeat (keeps the device "online"), queue counters, latest app version, recent SMS usage |
| `api/add-data` | Uploads one payment SMS (`address`, `message`) |
| `api/device-disconnect` | Releases the key so it can be used on another phone |

## Build

The APK is built by GitHub Actions on every push to `main` (see `.github/workflows/build-apk.yml`).
The latest APK is attached to the release `v<versionName>` and also kept on the `builds` branch.

Local build (Android SDK 35 + JDK 17): `./gradlew assembleRelease`

### Signing

Builds are signed with `keystore/paybondhu-public.jks`. Because this repository is public, that key is **not secret**:
it only makes sure every build installs over the previous one. For a private key, create a keystore and add the
repository secrets `PB_KEYSTORE_BASE64`, `PB_KEYSTORE_PASSWORD`, `PB_KEY_ALIAS` and `PB_KEY_PASSWORD`; the workflow
uses them automatically. (Switching keys needs a one-time uninstall/reinstall on the phones.)

### Releasing a new version

1. Raise `versionCode` / `versionName` in `app/build.gradle.kts`.
2. Raise `versionCode` / `versionName` in the website's `app/Config/AndroidApp.php` and upload the new APK to
   `public/assets/downloads/PayBondhu.apk`. Phones then show an "Update available" card.

## Releasing an update (automatic)

1. Edit `app/build.gradle.kts`: raise `versionCode` (must be higher than before) and set `versionName`.
2. Commit and push to `main`.
3. GitHub Actions builds the signed APK, publishes it as release `v<versionName>` and writes
   `latest.json` to the `builds` branch.
4. paybondhu.com checks `latest.json` at most every 20 minutes (on a phone heartbeat or `/cron`),
   verifies the SHA-256 and replaces `public/assets/downloads/PayBondhu.apk`.
5. Phones see the higher version in `api/device-status` and show **Update available** with a download button.

Keep `applicationId` and the signing key the same, otherwise Android refuses to install over the old version.
