# 🐑 Pontoufle Intercom

A tiny, extra-fluffy Android home-screen voice widget for Muse.

**Squish the blob → speak → hand off to Muse. Copy or share a reply → Pontoufle reads it aloud.**

## v0.4.0 — Pontoufle reads replies 🩷

- Keeps the existing **SQUISH TO TALK** speech-to-text Muse handoff intact.
- Adds **READ COPIED REPLY** in the app and **READ REPLY** on the home-screen widget.
- Shares text **to** Pontoufle: when an app exposes Share for selected reply text, choose **Pontoufle Intercom** to hear it.
- Reads the phone clipboard **only when the operator deliberately presses the read button**. No background polling.
- Uses Pontoufle's original squeaky Android voice: pitch **1.42**, rate **0.82** (written name Pontoufle, spoken name Pontoof).
- Queues long replies in speech-safe chunks. Includes **STOP SPEAKING**.
- No private Muse API, notification snooping, Accessibility Service, scraping, or background access to other apps.

### How to use reply playback

1. Squish **SQUISH TO TALK** and speak a message as usual. Pontoufle sends it to Muse.
2. When Muse responds, **copy the text of the reply**.
3. Tap Pontoufle's **🔊 READ REPLY** widget button or open Pontoufle and tap **READ COPIED REPLY**.
4. Pontoufle reads the copied text aloud in her existing voice.
5. If Muse offers **Share** on a reply, you can choose **Pontoufle Intercom** from the Android share sheet instead of copying.

**Limit:** This is user-assisted, two-way voice interaction. It cannot automatically fetch Muse's replies, because an approved reply API or other supported channel has not been verified. Sharing/copying the response is required. The clipboard may hold unrelated text, so only squish READ REPLY after copying what you want spoken.

### Test checklist

- Existing voice handoff to Muse still works from **SQUISH TO TALK**.
- Copy a short Muse reply, then tap **READ REPLY** in widget; it speaks once.
- Share text/plain from another app to Pontoufle; it speaks without the boot greeting.
- Copy a longer paragraph and confirm chunks are read in order.
- Tap **STOP SPEAKING** in the app; sound stops.
- Clear clipboard, then tap **READ REPLY**; a helpful message appears.
- Confirm that Pontoufle does not read the clipboard until a read button is tapped.

### Build

Open in Android Studio (JDK 17, Android SDK 35) and build/install the app module. It uses Android Gradle Plugin 8.7.3. The GitHub workflow also builds a **debug APK** as an artifact for convenient testing.

Package: `ai.emtizzle.pontoufleintercom`.

**Install the upgrade over the existing app** to preserve your home-screen widget. The v0.4 package name is unchanged. You may need to resize the widget slightly to see the new second button.

## Prior releases

### v0.3.1 — Say her name correctly
- Keeps the written Pontoufle brand while giving Android TTS the phonetic “Pontoof” pronunciation.

### v0.3 — The intercom connects
- Automatically shares each captured voice request directly to Muse.
- Uses Android's standard text-sharing interface with Muse's verified app package.
- Falls back to copying the request and opening Muse if direct sharing is unavailable.
- The OPEN MUSE button retransmits the latest captured request.

### v0.2 — Pontoufle has a voice
- High, bright clockwork pitch with a slower, sultry cadence.
- Boot transmission: “Pontoufle online, darling. Who are we bothering today?”
- Widget launches wait for her greeting before opening the microphone.

### v0.1
- Real Android home-screen widget.
- Runtime microphone permission, speech transcript preview.
- No Accessibility Service, no UI scraping, no access to Muse private data.

## Doctrine
**SQUISH THE BLOB. RESPECT THE SANDBOX. MAXIMUM FLOOF.**
