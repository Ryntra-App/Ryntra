# Notification setup

Ryntra offers two independent notification modes:

- Local checks keep the normal Modrinth session on the device. Android WorkManager and iOS BackgroundTasks decide when a check can run.
- Instant delivery uses an optional backend relay and a separate Modrinth OAuth grant limited to `USER_READ NOTIFICATION_READ`. The normal app session is never uploaded.

Disabling instant delivery removes the push token, restricted Modrinth token, user ID, and delivery history from the relay database.

## 1. Backend

Use the companion `rinthy-auth-backend` repository.

Create PostgreSQL, copy the variables from `.env.example` into the deployment secret store, then run:

```bash
npm ci
npm run db:migrate
npm run check
```

Required relay variables:

```text
PUBLIC_BASE_URL=https://auth.example.com
NOTIFICATION_MODRINTH_REDIRECT_URI=https://auth.example.com/api/notifications/oauth/callback
NOTIFICATION_APP_REDIRECT_URI=ryntra://notifications/callback
DATABASE_URL=postgres://...
DATABASE_SSL=require
TOKEN_ENCRYPTION_KEY=<32 random bytes encoded as base64>
CRON_SECRET=<at least 32 random URL-safe characters>
```

Keep `TOKEN_ENCRYPTION_KEY`, `CRON_SECRET`, database credentials, Firebase service-account JSON, and the APNs private key only in the server/Codemagic secret store. Never add them to either repository.

## 2. Modrinth OAuth

Add both HTTPS callbacks to the Modrinth OAuth application:

```text
https://auth.example.com/api/modrinth/callback
https://auth.example.com/api/notifications/oauth/callback
```

Set the matching backend environment variables. The notification callback deliberately requests only `USER_READ NOTIFICATION_READ`.

## 3. Android FCM

Create an Android app with package `com.ryntra.mobile` in Firebase. Give the backend service account permission to send Firebase Cloud Messaging messages and set:

```text
FIREBASE_PROJECT_ID=...
FIREBASE_SERVICE_ACCOUNT_JSON={...}
```

The Android app initializes Firebase programmatically and does not require a committed `google-services.json`. Add these non-secret client values to `local.properties`, Gradle properties, the shell environment, or Codemagic variables:

```text
RYNTRA_NOTIFICATION_BACKEND_URL=https://auth.example.com
RYNTRA_FIREBASE_API_KEY=...
RYNTRA_FIREBASE_APPLICATION_ID=1:...:android:...
RYNTRA_FIREBASE_PROJECT_ID=...
RYNTRA_FIREBASE_SENDER_ID=...
```

Without all four Firebase client values, the app hides instant setup as unavailable while local checks continue to work.

## 4. iOS APNs

In Apple Developer:

1. Enable Push Notifications for the `com.ryntra.mobile` App ID.
2. Regenerate the development/distribution provisioning profiles.
3. Create an APNs `.p8` key and note its key ID and team ID.
4. Configure the backend:

```text
APNS_BUNDLE_ID=com.ryntra.mobile
APNS_KEY_ID=...
APNS_TEAM_ID=...
APNS_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
APNS_USE_SANDBOX=false
```

Use `APNS_USE_SANDBOX=true` only when the installed app has a development APNs entitlement. Release/Codemagic device builds use `production`.

An unsigned IPA re-signed by Sideloadly can run the app, but instant APNs delivery works only if the resulting provisioning profile includes the Push Notifications entitlement. Local checks do not require APNs.

## 5. VPS and domain

The backend includes a standalone Node server:

```bash
HOST=127.0.0.1 PORT=3000 npm start
```

Run it with systemd, keep PostgreSQL private, and terminate HTTPS at Caddy or Nginx. Call the protected poll endpoint every one to five minutes:

```cron
*/2 * * * * curl --fail --silent --show-error -H "Authorization: Bearer $CRON_SECRET" https://auth.example.com/api/notifications/poll >/dev/null
```

After changing the domain:

1. Update backend `PUBLIC_BASE_URL`, `MODRINTH_REDIRECT_URI`, and `NOTIFICATION_MODRINTH_REDIRECT_URI`.
2. Update both callback URLs in the Modrinth OAuth application.
3. Set Android/Codemagic `RYNTRA_NOTIFICATION_BACKEND_URL`.
4. Set the iOS Xcode build setting `RYNTRA_NOTIFICATION_BACKEND_URL` or the Codemagic variable.
5. Verify `https://auth.example.com/api/health` before enabling the cron job.

## Privacy verification

The relay database never stores the normal Ryntra session. Restricted OAuth tokens are AES-256-GCM encrypted, installation secrets are one-way hashed, and notification bodies are not persisted. Database administrators can still access encrypted rows and server operators control the encryption key, so operational access to the VPS and secret store must remain restricted and audited.
