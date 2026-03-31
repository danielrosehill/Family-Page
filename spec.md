# Family Pager — Specification

## Overview

A minimal Android app (distributed via APK) that acts as a two-button pager between partners/spouses. It sends Pushover notifications at predefined urgency levels with a single tap. Optionally, it can also *receive* pages as device notifications.

## Use Case

Parents (or any couple) who need a dead-simple, always-available way to page each other — no chat, no typing, just "call me" or "emergency" at the press of a button.

---

## Core Features

### 1. Main Screen — The Pager

Two large, prominent buttons filling the screen:

| Button | Label | Pushover Priority | Sound | Behavior |
|--------|-------|-------------------|-------|----------|
| **Emergency / SOS** | `🚨 EMERGENCY` | `2` (Emergency — repeats until acknowledged) | `siren` | Sends with `retry: 60`, `expire: 3600` |
| **Call Me ASAP** | `📞 Call Me ASAP` | `1` (High — bypasses quiet hours) | `persistent` | Single high-priority notification |

**Message format:**
- Emergency: `Family Pager: {your_name} — EMERGENCY / SOS!`
- Call Me ASAP: `Family Pager: {your_name} — Call me ASAP`

The title for both: `Family Pager`

After sending, show a brief confirmation toast ("Page sent!") or error feedback.

### 2. Settings Screen

Accessible via a gear icon on the main screen. Persisted in SharedPreferences (encrypted via EncryptedSharedPreferences).

| Setting | Description | Required |
|---------|-------------|----------|
| **Your name** | Sender's name (appears in the page message) | Yes |
| **Spouse's name** | Recipient's name (used in UI labels) | Yes |
| **App API Token** | Pushover Application API token (30-char alphanumeric) | Yes |
| **Spouse's User Key** | Pushover User Key of the recipient (30-char alphanumeric) | Yes |
| **Your User Key** | Your own Pushover User Key (needed only if "Receive Pages" is enabled) | Conditional |
| **Your Pushover Email** | Pushover account email (needed only if "Receive Pages" is enabled) | Conditional |
| **Your Pushover Password** | Pushover account password (for Open Client login; stored encrypted, used once to obtain session secret) | Conditional |
| **Receive Pages** | Toggle: register as an Open Client device and show incoming pages as Android notifications | No (default off) |

### 3. Receive Pages (Optional)

When enabled:
1. **Login** — POST to `https://api.pushover.net/1/users/login.json` with email + password (+ 2FA if needed) to obtain a `secret` session token.
2. **Register device** — POST to `https://api.pushover.net/1/devices.json` with `secret`, `name` (e.g., `familypager_<device>`), `os: "O"`.
3. **WebSocket listener** — Connect to `wss://client.pushover.net/push`, authenticate with `login:<device_id>:<secret>\n`, and listen:
   - `#` → keep-alive (ignore)
   - `!` → new message → fetch via GET `https://api.pushover.net/1/messages.json?secret=...&device_id=...` → show as Android notification
   - `R` → reconnect
   - `E` → re-authenticate
   - `A` → device conflict (warn user)
4. After displaying, delete messages via POST to `https://api.pushover.net/1/devices/<device_id>/update_highest_message.json`.

This runs as a **foreground service** with a persistent notification ("Family Pager is listening").

---

## Pushover API Reference

### Send Message
- **Endpoint:** `POST https://api.pushover.net/1/messages.json`
- **Required:** `token` (app API token), `user` (recipient user key), `message`
- **Used optional params:** `title`, `priority`, `sound`, `retry` (for priority 2), `expire` (for priority 2)

### Validate User Key
- **Endpoint:** `POST https://api.pushover.net/1/users/validate.json`
- **Params:** `token`, `user`
- Useful for settings validation before saving.

---

## Technical Decisions

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| Language | Kotlin | Modern Android standard |
| UI | Jetpack Compose + Material 3 | Declarative, less boilerplate |
| Min SDK | 26 (Android 8.0) | Covers 95%+ of devices, needed for notification channels |
| HTTP client | OkHttp | Lightweight, WebSocket support built-in |
| Storage | EncryptedSharedPreferences | Secure storage for API keys |
| Background | Foreground Service (only when receiving) | Required for persistent WebSocket |
| Distribution | Direct APK (sideload) | No Play Store needed |

## Permissions

- `INTERNET` — API calls and WebSocket
- `POST_NOTIFICATIONS` (Android 13+) — Show notifications
- `FOREGROUND_SERVICE` — For the WebSocket listener service
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — Keep alive when receiving pages
- `WAKE_LOCK` — Ensure WebSocket reconnection

## Architecture

```
app/
├── MainActivity.kt              — Entry point, nav host
├── ui/
│   ├── theme/Theme.kt           — Material 3 theming (dark/light)
│   ├── PagerScreen.kt           — Main 2-button screen
│   └── SettingsScreen.kt        — Configuration form
├── data/
│   └── SettingsRepository.kt    — EncryptedSharedPreferences wrapper
├── network/
│   └── PushoverApi.kt           — Send message, validate user, Open Client API
├── service/
│   └── PushoverListenerService.kt — Foreground service with WebSocket
└── notifications/
    └── NotificationHelper.kt    — Channel creation, notification display
```

## UI Design

- **Color scheme:** Bold red for Emergency, amber/orange for Call Me ASAP, dark background
- **Typography:** Large, readable text — this may be used in a panic
- **Layout:** Buttons should be large enough to tap without precision (at least 50% of screen height each)
- **Haptic feedback:** Vibrate on button press for tactile confirmation
- **Confirmation:** Brief toast after successful send; snackbar with retry on failure
- **Settings:** Standard form with input validation and a "Test" button to verify the configuration
