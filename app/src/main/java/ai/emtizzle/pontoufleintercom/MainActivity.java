package ai.emtizzle.pontoufleintercom;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ActivityNotFoundException;
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

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int SPEECH = 41;
    private static final int MIC = 42;
    private static final String BOOT_UTTERANCE = "pontoufle_boot";
    private static final String BOOT_LINE =
            "Pontoof online, darling. Who are we bothering today?";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView transcript;
    private TextToSpeech voice;
    private boolean greetOnVoiceReady;
    private boolean listenAfterGreeting;
    private String capturedRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        transcript = findViewById(R.id.transcript);
        findViewById(R.id.talk).setOnClickListener(v -> listen());
        findViewById(R.id.openMuse).setOnClickListener(v -> openMuse());

        listenAfterGreeting = getIntent().getBooleanExtra("listen_now", false);
        greetOnVoiceReady = savedInstanceState == null;
        voice = new TextToSpeech(this, this);

        if (!greetOnVoiceReady && listenAfterGreeting) {
            listen();
        }
    }

    @Override
    public void onInit(int result) {
        if (result != TextToSpeech.SUCCESS) {
            status.setText("PONTOUFLE'S VOICEBOX IS WARMING UP…");
            if (listenAfterGreeting) {
                listen();
            }
            return;
        }

        voice.setLanguage(Locale.getDefault());
        voice.setPitch(1.42f);
        voice.setSpeechRate(0.82f);
        voice.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }

            @Override
            public void onDone(String utteranceId) {
                if (BOOT_UTTERANCE.equals(utteranceId) && listenAfterGreeting) {
                    mainHandler.post(MainActivity.this::listen);
                }
            }

            @Override
            public void onError(String utteranceId) {
                if (BOOT_UTTERANCE.equals(utteranceId) && listenAfterGreeting) {
                    mainHandler.post(MainActivity.this::listen);
                }
            }
        });

        if (greetOnVoiceReady) {
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
            clipboard.setPrimaryClip(ClipData.newPlainText("Pontoufle request", request));
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

    @Override
    protected void onDestroy() {
        if (voice != null) {
            voice.stop();
            voice.shutdown();
        }
        super.onDestroy();
    }
}
