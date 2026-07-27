# Notifications

Ryntra supports two notification modes.

## Local background checks

Local checks use the active Modrinth session stored on the device. Android
WorkManager and iOS BackgroundTasks periodically look for new notifications and
present them through the operating system.

The exact delivery time is controlled by Android or iOS and may be delayed by
battery-saving restrictions. Local checks do not require the Ryntra relay,
Firebase Cloud Messaging, or Apple Push Notification service.

## Instant server notifications

Instant notifications use the deployed Ryntra relay at
`https://authrinthy.sawiq.org`.

The user enables this mode from the notification settings and completes a
separate Modrinth authorization limited to:

```text
USER_READ NOTIFICATION_READ
```

The normal application session is never uploaded to the relay. After instant
delivery is connected, local background polling is disabled to avoid duplicate
notifications.

Disconnecting instant notifications removes the installation, restricted
Modrinth token, push token, user ID, and delivery history from the relay.

## Platform behavior

### Android

Android instant delivery uses Firebase Cloud Messaging. The production Android
build already contains the non-secret Firebase client configuration and the
Ryntra relay URL.

The user only needs to grant the Android notification permission and enable
instant notifications inside Ryntra.

### iOS

iOS instant delivery uses Apple Push Notification service. The application has
the required push entitlement, but the profile used to sign the installed app
must also include Push Notifications.

An unsigned IPA re-signed with a profile that does not grant this entitlement
can still run normally, but instant notifications will not arrive. Local
background checks remain available.

## Troubleshooting

- Confirm that notification permission is enabled for Ryntra in the system
  settings.
- Open Ryntra settings and check whether instant notifications show as
  connected.
- If the authorization expired, disconnect instant notifications and connect
  them again.
- Confirm that `https://authrinthy.sawiq.org/api/health` returns a successful
  response.
- On Android, check that Google Play services are available and battery
  restrictions are not blocking the app.
- On iOS, verify that the installed provisioning profile contains the
  `aps-environment` entitlement.
- If instant delivery is unavailable, enable local background checks instead.

## Privacy

The relay never stores the normal Ryntra session. Restricted OAuth tokens are
encrypted, installation secrets are stored as one-way hashes, and notification
bodies are not persisted.

Server deployment details and production secrets belong in the private
deployment configuration of the companion backend, not in the mobile
application repository.
