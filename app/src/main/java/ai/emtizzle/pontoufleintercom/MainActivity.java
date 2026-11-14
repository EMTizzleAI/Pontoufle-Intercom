package ai.emtizzle.pontoufleintercom;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Pontoufle is a voice handoff to Muse, plus an opt-in reader for copied/shared replies.
 *
 * No background clipboard monitoring, notification access, Accessibility Service, or
 * scraping is used. Muse has to share text to this Activity, or the operator explicitly
 * copies a reply and taps READ REPLY.
 */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int SPEECH = 41;
    private static final int MIC = 42;

    static final String ACTION_TALK = "ai.emtizzle.pontoufleintercom.TALK";
    static final String ACTION_READ_REPLY = "ai.emtizzle.pontoufleintercom.READ_REPLY";

    private static final String BOOT_UTTERANCE = "pontoufle_boot";
    private static final String BOOT_LINE =
            "Pontoof online, darling. Who are we bothering today?";
    private static final int MAX_REPLY_CHARS = 15000;
    private static final int REPLY_PREVIEW_CHARS = 700;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView transcript;
    private TextToSpeech voice;
    private boolean voiceReady;
    private boolean greetOnVoiceReady;
    private boolean listenAfterGreeting;
    private String pendingReply;
    private String capturedRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        transcript = findViewById(R.id.transcript);
        findViewById(R.id.talk).setOnClickListener(v -> listen());
        findViewById(R.id.openMuse).setOnClickListener(v -> openMuse());
        findViewById(R.id.readReply).setOnClickListener(v -> readCopiedReply());
        findViewById(R.id.stopVoice).setOnClickListener(v -> stopSpeaking());

        Intent launch = getIntent();
        boolean sharedText = isSharedText(launch);
        boolean readFromWidget = ACTION_READ_REPLY.equals(launch.getAction());
        listenAfterGreeting = launch.getBooleanExtra("listen_now", false)
                || ACTION_TALK.equals(launch.getAction());
        greetOnVoiceReady = savedInstanceState == null
                && !sharedText && !readFromWidget;

        voice = new TextToSpeech(this, this);

        if (sharedText) {
            receiveSharedReply(launch);
        } else if (readFromWidget) {
            // Clipboard is accessed only after a deliberate tap on the widget.
            mainHandler.post(this::readCopiedReply);
        } else if (!greetOnVoiceReady && listenAfterGreeting) {
            listen();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (isSharedText(intent)) {
            receiveSharedReply(intent);
        } else if (ACTION_READ_REPLY.equals(intent.getAction())) {
            readCopiedReply();
        } else if (ACTION_TALK.equals(intent.getAction())
                || intent.getBooleanExtra("listen_now", false)) {
            listen();
        }
    }

    private boolean isSharedText(Intent intent) {
        return intent != null
                && Intent.ACTION_SEND.equals(intent.getAction())
                && "text/plain".equals(intent.getType());
    }

    @Override
    public void onInit(int result) {
        mainHandler.post(() -> finishVoiceInit(result));
    }

    private void finishVoiceInit(int result) {
        if (result != TextToSpeech.SUCCESS || voice == null) {
            status.setText("PONTOUFLE'S VOICEBOX IS UNAVAILABLE");
            if (listenAfterGreeting) {
                listen();
            }
            return;
        }

        voiceReady = true;
        voice.setLanguage(Locale.getDefault());
        // Preserve Pontoufle's original squeaky, bright clockwork voice.
        voice.setPitch(1.42f);
        voice.setSpeechRate(0.82f);
        voice.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                if (BOOT_UTTERANCE.equals(utteranceId) && listenAfterGreeting) {
                    mainHandler.post(() -> {
                        listenAfterGreeting = false;
                        listen();
                    });
                }
            }

            @Override
            public void onError(String utteranceId) {
                if (BOOT_UTTERANCE.equals(utteranceId) && listenAfterGreeting) {
                    mainHandler.post(() -> {
                        listenAfterGreeting = false;
                        listen();
                    });
                }
            }
        });

        if (pendingReply != null) {
            String reply = pendingReply;
            pendingReply = null;
            speakReply(reply);
        } else if (greetOnVoiceReady) {
            status.setText("PONTOUFLE SIGNAL ACQUIRED ✨");
            voice.speak(BOOT_LINE, TextToSpeech.QUEUE_FLUSH, null, BOOT_UTTERANCE);
        }
    }

    private void listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell Pontoufle what you need…");
        status.setText("PONTOUFLE IS LISTENING…");

        try {
            startActivityForResult(intent, SPEECH);
        } catch (ActivityNotFoundException error) {
            status.setText("THE FLUFF CANNOT HEAR YOU 💀");
            transcript.setText("No speech recognition service is available.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            listen();
        } else if (requestCode == MIC) {
            status.setText("MICROPHONE DENIED — FLOOF RESPECTS BOUNDARIES");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> words =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (words != null && !words.isEmpty()) {
                capturedRequest = words.get(0);
                transcript.setText(capturedRequest);
                status.setText("TRANSMITTING TO MUSE ✨");
                sendToMuse(capturedRequest);
            }
        }
    }

    private void openMuse() {
        if (capturedRequest != null && !capturedRequest.trim().isEmpty()) {
            sendToMuse(capturedRequest);
            return;
        }
        launchMuse();
    }

    private void sendToMuse(String request) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, request);
        share.setPackage("com.facebook.aura");

        try {
            startActivity(share);
            status.setText("REQUEST DELIVERED TO MUSE ✨");
        } catch (ActivityNotFoundException error) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Pontoufle request", request));
            }
            status.setText("REQUEST COPIED — PASTE IN MUSE ✨");
            Toast.makeText(this,
                    "Pontoufle copied your request. Paste it into Muse.",
                    Toast.LENGTH_LONG).show();
            launchMuse();
        }
    }

    private void launchMuse() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://applink.muse.ai"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            status.setText("THE FLUFF HAS LOST CONTACT WITH COMMAND");
        }
    }

    private void receiveSharedReply(Intent intent) {
        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (shared == null || shared.toString().trim().isEmpty()) {
            status.setText("NO SHARED TEXT FOUND");
            transcript.setText("Copy Muse's reply, then tap READ COPIED REPLY.");
            return;
        }
        queueReply(shared.toString());
    }

    private void readCopiedReply() {
        // Android allows foreground clipboard reading. This is intentionally user-triggered.
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            status.setText("NOTHING COPIED YET 🐑");
            transcript.setText("Copy the reply in Muse, then squish READ REPLY again.");
            return;
        }

        ClipData data = clipboard.getPrimaryClip();
        if (data == null || data.getItemCount() == 0) {
            status.setText("NOTHING COPIED YET 🐑");
            return;
        }

        CharSequence copied = data.getItemAt(0).coerceToText(this);
        if (copied == null || copied.toString().trim().isEmpty()) {
            status.setText("NO TEXT IN CLIPBOARD");
            return;
        }
        queueReply(copied.toString());
    }

    private void queueReply(String rawText) {
        String reply = rawText.trim();
        if (reply.isEmpty()) {
            status.setText("EMPTY REPLY");
            return;
        }
        if (reply.length() > MAX_REPLY_CHARS) {
            reply = reply.substring(0, MAX_REPLY_CHARS);
            Toast.makeText(this,
                    "Long reply shortened for audio playback.", Toast.LENGTH_LONG).show();
        }

        transcript.setText(reply.length() > REPLY_PREVIEW_CHARS
                ? reply.substring(0, REPLY_PREVIEW_CHARS) + "…"
                : reply);
        status.setText("PONTOUFLE RECEIVED THE REPLY ✨");

        if (!voiceReady || voice == null) {
            pendingReply = reply;
            return;
        }
        speakReply(reply);
    }

    private void speakReply(String reply) {
        if (!voiceReady || voice == null) {
            pendingReply = reply;
            return;
        }

        // Android TTS has a per-utterance length limit. Queue smaller word-boundary chunks.
        int chunkMax = Math.min(2800, TextToSpeech.getMaxSpeechInputLength() - 1);
        int position = 0;
        int sequence = 0;
        while (position < reply.length()) {
            int end = Math.min(position + chunkMax, reply.length());
            if (end < reply.length()) {
                int boundary = reply.lastIndexOf(' ', end);
                if (boundary > position + chunkMax / 2) {
                    end = boundary + 1;
                }
            }
            String part = reply.substring(position, end).trim();
            if (!part.isEmpty()) {
                voice.speak(part, sequence == 0
                                ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
                        null, "pontoufle_reply_" + System.nanoTime() + "_" + sequence);
                sequence++;
            }
            position = end;
        }
        status.setText("PONTOUFLE IS READING ALOUD 🐑");
    }

    private void stopSpeaking() {
        pendingReply = null;
        if (voice != null) {
            voice.stop();
        }
        status.setText("PONTOUFLE HAS PAUSED ✨");
    }

    @Override
    protected void onDestroy() {
        if (voice != null) {
            voice.stop();
            voice.shutdown();
        }
        super.onDestroy();
    }
}
