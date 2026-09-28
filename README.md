# 🐑 Pontoufle Intercom

A tiny, extra-fluffy Android home-screen voice widget for Muse.

**Squish the blob → speak → capture the request → cleanly hand off to Muse.**

## v0.3.1 — Say her name correctly
- Keeps the written Pontoufle brand while giving Android TTS the phonetic “Pontoof” pronunciation

## v0.3 — The intercom connects
- Automatically shares each captured voice request directly to Muse
- Uses Android's standard text-sharing interface with Muse's verified app package
- Falls back to copying the request and opening Muse if direct sharing is unavailable
- The OPEN MUSE button retransmits the latest captured request

## v0.2 — Pontoufle has a voice
- High, bright clockwork pitch with a slower, sultry cadence
- Boot transmission: “Pontoufle online, darling. Who are we bothering today?”
- Widget launches wait for her greeting before opening the microphone
- Keeps the existing first-boot radio crackle intact

## v0.1
- Real Android home-screen widget
- Extra-floofy Pontoufle mascot
- One-tap speech capture
- Runtime microphone permission
- Transcript preview
- Muse handoff through a normal verified web-link surface
- No Accessibility Service
- No UI scraping
- No access to Muse private data

The current Muse handoff opens Muse's verified app-link surface. Automatic prompt injection / response capture is intentionally not claimed until a supported interface is verified.

## Build
Open in Android Studio with JDK 17 and build/install the app module.

Package: ai.emtizzle.pontoufleintercom

## Doctrine
**SQUISH THE BLOB. RESPECT THE SANDBOX. MAXIMUM FLOOF.**
