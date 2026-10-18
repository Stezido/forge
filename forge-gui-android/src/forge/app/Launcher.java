package forge.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import forge.gui.GuiBase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Launcher extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.widget.TextView message = new android.widget.TextView(this);
        message.setText("Preparing Chaos Brawl resources…\nFirst launch can take a few minutes.");
        message.setTextColor(android.graphics.Color.rgb(238, 231, 215));
        message.setBackgroundColor(android.graphics.Color.rgb(17, 20, 17));
        message.setTextSize(20);
        message.setGravity(android.view.Gravity.CENTER);
        message.setPadding(32, 32, 32, 32);
        setContentView(message);
        new Thread(() -> {
            try {
                java.io.File destination = new java.io.File(getFilesDir(), "forge.app.chaosbrawl");
                try (InputStream bundle = getAssets().open("chaosbrawl-resources.zip");
                     InputStream digest = getAssets().open("chaosbrawl-resources.sha256")) {
                    String expected = new BufferedReader(new InputStreamReader(digest, java.nio.charset.StandardCharsets.US_ASCII)).readLine().trim();
                    ChaosBrawlResources.install(bundle, destination, expected);
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Intent main = new Intent(Launcher.this, Main.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(main);
                    Intent intent = getIntent();
                    sendIntent(intent, intent.getAction(), intent.getType());
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
            } catch (Exception failure) {
                android.util.Log.e("ChaosBrawl", "Bundled resources could not be prepared", failure);
                runOnUiThread(() -> message.setText("Could not prepare Chaos Brawl. Check free storage, then close and reopen the app. Your saved games have been kept."));
            }
        }, "chaosbrawl-resources").start();
    }

    private void sendIntent(Intent intent, String action, String type) {
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                if ("text/plain".equals(type)) {
                    Uri textUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (textUri != null) {
                        try {
                            InputStream in = getContentResolver().openInputStream(textUri);
                            BufferedReader r = new BufferedReader(new InputStreamReader(in));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            GuiBase.getInterface().copyToClipboard(total.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            GuiBase.getInterface().copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1500);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if ("text/plain".equals(type)) {
                    Uri textUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (textUri != null) {
                        try {
                            InputStream in = getContentResolver().openInputStream(textUri);
                            BufferedReader r = new BufferedReader(new InputStreamReader(in));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            GuiBase.getInterface().copyToClipboard(total.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            GuiBase.getInterface().copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1500);
        }
    }
}
