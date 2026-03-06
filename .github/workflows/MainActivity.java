package com.lumixremote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // ── UI References ──────────────────────────────────────────────────
    private EditText ipInput;
    private TextView statusText, logText, recIndicator;
    private TextView isoVal, ssVal, fVal, evVal;
    private ImageView liveViewImg;
    private LinearLayout lvPlaceholder;
    private ScrollView logScroll;
    private Button btnShutter;

    // ── State ──────────────────────────────────────────────────────────
    private String camIP = "192.168.54.1";
    private boolean isConnected = false;
    private boolean isRecording = false;
    private boolean lvRunning = false;
    private int recSeconds = 0;

    // Settings
    private final String[] isoValues = {"AUTO","100","200","400","800","1600","3200","6400","12800","25600"};
    private final String[] ssValues  = {"1/4000","1/2000","1/1000","1/500","1/250","1/125","1/60","1/30","1/15","1/8","1/4","1/2","1\"","2\"","4\""};
    private final String[] fValues   = {"f/3.5","f/4","f/4.5","f/5","f/5.6","f/6.3","f/7.1","f/8","f/9","f/10","f/11"};
    private final String[] evValues  = {"-3.0","-2.7","-2.3","-2.0","-1.7","-1.3","-1.0","-0.7","-0.3","±0","+0.3","+0.7","+1.0","+1.3","+1.7","+2.0","+2.3","+2.7","+3.0"};
    private int isoIdx = 0, ssIdx = 6, fIdx = 0, evIdx = 9;

    // ── Executors ──────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable liveViewRunnable;
    private Runnable recTimerRunnable;

    // ── Lifecycle ──────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupListeners();
        updateSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lvRunning = false;
        executor.shutdownNow();
    }

    // ── Bind Views ─────────────────────────────────────────────────────
    private void bindViews() {
        ipInput      = findViewById(R.id.ipInput);
        statusText   = findViewById(R.id.statusText);
        logText      = findViewById(R.id.logText);
        logScroll    = findViewById(R.id.logScroll);
        liveViewImg  = findViewById(R.id.liveViewImg);
        lvPlaceholder= findViewById(R.id.lvPlaceholder);
        recIndicator = findViewById(R.id.recIndicator);
        btnShutter   = findViewById(R.id.btnShutter);
        isoVal = findViewById(R.id.isoVal);
        ssVal  = findViewById(R.id.ssVal);
        fVal   = findViewById(R.id.fVal);
        evVal  = findViewById(R.id.evVal);
    }

    // ── Setup Listeners ────────────────────────────────────────────────
    private void setupListeners() {
        findViewById(R.id.btnConnect).setOnClickListener(v -> connect());
        btnShutter.setOnClickListener(v -> capturePhoto());
        btnShutter.setOnLongClickListener(v -> { halfPress(); return true; });

        findViewById(R.id.btnRecStart).setOnClickListener(v -> startVideo());
        findViewById(R.id.btnRecStop).setOnClickListener(v -> stopVideo());
        findViewById(R.id.btnAF).setOnClickListener(v -> autofocus());
        findViewById(R.id.btnLvStart).setOnClickListener(v -> startLiveView());
        findViewById(R.id.btnLvStop).setOnClickListener(v -> stopLiveView());
        findViewById(R.id.btnClearLog).setOnClickListener(v -> clearLog());
        findViewById(R.id.btnZoomW).setOnClickListener(v -> sendCmd("cam.cgi?mode=camcmd&value=zoom_wide_fast"));
        findViewById(R.id.btnZoomT).setOnClickListener(v -> sendCmd("cam.cgi?mode=camcmd&value=zoom_tele_fast"));

        // ISO
        findViewById(R.id.btnIsoDec).setOnClickListener(v -> { if (isoIdx > 0) { isoIdx--; updateSettings(); sendSetting("iso", isoValues[isoIdx]); } });
        findViewById(R.id.btnIsoInc).setOnClickListener(v -> { if (isoIdx < isoValues.length-1) { isoIdx++; updateSettings(); sendSetting("iso", isoValues[isoIdx]); } });
        // Shutter Speed
        findViewById(R.id.btnSsDec).setOnClickListener(v -> { if (ssIdx > 0) { ssIdx--; updateSettings(); sendSetting("shtrspeed", ssValues[ssIdx]); } });
        findViewById(R.id.btnSsInc).setOnClickListener(v -> { if (ssIdx < ssValues.length-1) { ssIdx++; updateSettings(); sendSetting("shtrspeed", ssValues[ssIdx]); } });
        // Aperture
        findViewById(R.id.btnFDec).setOnClickListener(v -> { if (fIdx > 0) { fIdx--; updateSettings(); sendSetting("focal", fValues[fIdx]); } });
        findViewById(R.id.btnFInc).setOnClickListener(v -> { if (fIdx < fValues.length-1) { fIdx++; updateSettings(); sendSetting("focal", fValues[fIdx]); } });
        // EV
        findViewById(R.id.btnEvDec).setOnClickListener(v -> { if (evIdx > 0) { evIdx--; updateSettings(); sendSetting("expcomp", evValues[evIdx]); } });
        findViewById(R.id.btnEvInc).setOnClickListener(v -> { if (evIdx < evValues.length-1) { evIdx++; updateSettings(); sendSetting("focal", evValues[evIdx]); } });
    }

    private void updateSettings() {
        isoVal.setText(isoValues[isoIdx]);
        ssVal.setText(ssValues[ssIdx]);
        fVal.setText(fValues[fIdx]);
        evVal.setText(evValues[evIdx]);
    }

    // ── Connection ─────────────────────────────────────────────────────
    private void connect() {
        camIP = ipInput.getText().toString().trim();
        if (camIP.isEmpty()) camIP = "192.168.54.1";
        log("Mencoba ke " + camIP + "...", LogType.INFO);

        executor.execute(() -> {
            // Lumix handshake: request access with a UUID
            boolean ok = httpGet("cam.cgi?mode=accctrl&type=req_acc&value=4D454930-0100-1000-8000-F02765BACACE&value2=LumixRemote");
            mainHandler.post(() -> {
                if (ok) {
                    isConnected = true;
                    setStatus(true);
                    log("Terhubung! Siap kontrol kamera.", LogType.OK);
                } else {
                    isConnected = false;
                    setStatus(false);
                    log("Gagal. Pastikan kamera dalam mode WiFi Remote.", LogType.ERR);
                }
            });
        });
    }

    private void setStatus(boolean connected) {
        View dot = findViewById(R.id.statusDot);
        statusText.setText(connected ? "ONLINE" : "OFFLINE");
        statusText.setTextColor(connected ? Color.parseColor("#30C060") : Color.parseColor("#5A6878"));
        dot.setBackgroundResource(connected ? R.drawable.dot_online : R.drawable.dot_offline);
    }

    // ── Camera Commands ────────────────────────────────────────────────
    private void capturePhoto() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        log("Mengambil foto...", LogType.INFO);
        btnShutter.setAlpha(0.5f);
        mainHandler.postDelayed(() -> btnShutter.setAlpha(1f), 200);
        sendCmd("cam.cgi?mode=camcmd&value=capture");
        mainHandler.postDelayed(() -> log("Foto diambil!", LogType.OK), 500);
    }

    private void halfPress() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        log("Half press AF...", LogType.INFO);
        sendCmd("cam.cgi?mode=camcmd&value=af_s_push");
    }

    private void autofocus() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        log("Autofocus...", LogType.INFO);
        sendCmd("cam.cgi?mode=camcmd&value=af_s_push");
    }

    private void startVideo() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        if (isRecording) { log("Sudah merekam!", LogType.ERR); return; }
        log("Mulai rekam video...", LogType.INFO);
        executor.execute(() -> {
            boolean ok = httpGet("cam.cgi?mode=camcmd&value=video_recstart");
            mainHandler.post(() -> {
                if (ok) {
                    isRecording = true;
                    btnShutter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CC2020")));
                    recIndicator.setVisibility(View.VISIBLE);
                    recSeconds = 0;
                    startRecTimer();
                    log("Merekam...", LogType.OK);
                }
            });
        });
    }

    private void stopVideo() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        sendCmd("cam.cgi?mode=camcmd&value=video_recstop");
        isRecording = false;
        btnShutter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DDDDDD")));
        recIndicator.setVisibility(View.GONE);
        if (recTimerRunnable != null) mainHandler.removeCallbacks(recTimerRunnable);
        log("Video disimpan.", LogType.OK);
    }

    private void startRecTimer() {
        recTimerRunnable = new Runnable() {
            @Override public void run() {
                if (!isRecording) return;
                recSeconds++;
                int m = recSeconds / 60, s = recSeconds % 60;
                recIndicator.setText(String.format("⬤ REC  %02d:%02d", m, s));
                mainHandler.postDelayed(this, 1000);
            }
        };
        mainHandler.postDelayed(recTimerRunnable, 1000);
    }

    // ── Live View ──────────────────────────────────────────────────────
    private void startLiveView() {
        if (!isConnected) { log("Belum terhubung!", LogType.ERR); return; }
        if (lvRunning) return;
        lvRunning = true;
        // Start MJPEG stream command
        sendCmd("cam.cgi?mode=startstream&value=49152");
        log("Live view aktif...", LogType.OK);
        liveViewRunnable = new Runnable() {
            @Override public void run() {
                if (!lvRunning) return;
                executor.execute(() -> {
                    Bitmap bmp = fetchFrame();
                    mainHandler.post(() -> {
                        if (bmp != null) {
                            liveViewImg.setImageBitmap(bmp);
                            liveViewImg.setVisibility(View.VISIBLE);
                            lvPlaceholder.setVisibility(View.GONE);
                        }
                    });
                });
                if (lvRunning) mainHandler.postDelayed(this, 100); // ~10fps
            }
        };
        mainHandler.post(liveViewRunnable);
    }

    private void stopLiveView() {
        lvRunning = false;
        if (liveViewRunnable != null) mainHandler.removeCallbacks(liveViewRunnable);
        sendCmd("cam.cgi?mode=stopstream");
        liveViewImg.setVisibility(View.GONE);
        lvPlaceholder.setVisibility(View.VISIBLE);
        log("Live view dihentikan.", LogType.INFO);
    }

    private Bitmap fetchFrame() {
        try {
            URL url = new URL("http://" + camIP + "/cam.cgi?mode=getliveviewimage&t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                conn.disconnect();
                return bmp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private void sendCmd(String endpoint) {
        executor.execute(() -> httpGet(endpoint));
    }

    private void sendSetting(String type, String value) {
        if (!isConnected) return;
        sendCmd("cam.cgi?mode=setsetting&type=" + type + "&value=" + value);
        log("Set " + type + " → " + value, LogType.INFO);
    }

    private boolean httpGet(String endpoint) {
        try {
            URL url = new URL("http://" + camIP + "/" + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            mainHandler.post(() -> log("→ " + endpoint.substring(0, Math.min(endpoint.length(), 40)) + " [" + code + "]", LogType.OK));
            return (code == 200);
        } catch (Exception e) {
            mainHandler.post(() -> log("✗ " + e.getMessage(), LogType.ERR));
            return false;
        }
    }

    // ── Log ────────────────────────────────────────────────────────────
    enum LogType { OK, ERR, INFO }

    private void log(String msg, LogType type) {
        int color;
        switch (type) {
            case OK:   color = Color.parseColor("#30C060"); break;
            case ERR:  color = Color.parseColor("#E03030"); break;
            default:   color = Color.parseColor("#E8A020"); break;
        }
        SpannableString span = new SpannableString(msg + "\n");
        span.setSpan(new ForegroundColorSpan(color), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logText.append(span);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void clearLog() {
        logText.setText("[LOG] Dibersihkan\n");
    }
}
