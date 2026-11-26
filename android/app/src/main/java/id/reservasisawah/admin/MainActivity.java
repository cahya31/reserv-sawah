package id.reservasisawah.admin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.media.AudioManager;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://reserv-sawah.ayimasnoneng.workers.dev/";
    private static final String CHANNEL_ID = "reservasi_badge";
    private static final String EVENTS_CHANNEL_PREFIX = "reservasi_events_";
    private static final String PREFS_NAME = "reservasi_settings";
    private static final String PREF_TONE = "notification_tone";
    private static final int BADGE_NOTIFICATION_ID = 731;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 7310;
    private WebView webView;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Window window = getWindow();
        window.setStatusBarColor(0xFF176B45);
        window.setNavigationBarColor(0xFF103F2C);
        window.getDecorView().setSystemUiVisibility(0);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createBadgeChannel();
        createEventsChannel(getSelectedTone());
        requestNotificationPermissionIfNeeded();

        webView = new WebView(this);
        webView.addJavascriptInterface(new BadgeBridge(), "AndroidBadge");
        webView.addJavascriptInterface(new AndroidNotificationsBridge(), "AndroidNotifications");
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                return host != null && !host.equals("reserv-sawah.ayimasnoneng.workers.dev");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith(APP_URL)) {
                    view.evaluateJavascript(
                        "(function(){if(window.AndroidNotifications){window.enablePushNotifications=async function(){return 'Notifikasi Android berhasil diaktifkan. Pemberitahuan akan muncul saat aplikasi berjalan.';};window.showReservationNotification=function(title,body){try{window.AndroidNotifications.show(String(title||'Pemberitahuan Reservasi'),String(body||''));return true;}catch(e){return false;}};var b=document.getElementById('enablePush');if(b)b.textContent='🔔 Aktifkan Notifikasi Android';if(!document.getElementById('androidToneButton')){var t=document.createElement('button');t.id='androidToneButton';t.textContent='🎵 Ganti Nada Notifikasi';t.style.cssText='position:fixed;right:14px;bottom:18px;z-index:2147483647;background:#176b45;color:white;border:0;border-radius:24px;padding:12px 16px;font-weight:700;box-shadow:0 3px 12px #0004';t.onclick=function(){window.AndroidNotifications.showTonePicker();};document.body.appendChild(t);}}})();" +
                        "(function(){if(window.__sawahBadgeTimer)return;" +
                        "function check(){fetch('/api/me',{cache:'no-store',credentials:'include'})" +
                        ".then(function(r){if(!r.ok)throw new Error('auth');return r.json();})" +
                        ".then(function(me){if(!me.user||me.user.role!=='ADMIN'){if(window.AndroidBadge)window.AndroidBadge.setPendingCount(0);return null;}" +
                        "return fetch('/api/reservations',{cache:'no-store',credentials:'include'});})" +
                        ".then(function(r){if(!r)return null;if(!r.ok)throw new Error('auth');return r.json();})" +
                        ".then(function(d){if(!d)return;var rows=d.reservations||[];" +
                        "var pending=rows.filter(function(x){return x.status==='PENDING';}).length;" +
                        "if(window.AndroidBadge)window.AndroidBadge.setPendingCount(pending);})" +
                        ".catch(function(){});}" +
                        "check();window.__sawahBadgeTimer=setInterval(check,10000);})();",
                        null
                    );
                }
            }
        });
        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void createBadgeChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Penanda reservasi", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Menampilkan titik pada ikon aplikasi jika ada reservasi menunggu.");
            channel.setShowBadge(true);
            channel.setSound(null, null);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String getSelectedTone() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_TONE, "default");
    }

    private void createEventsChannel(String tone) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = EVENTS_CHANNEL_PREFIX + tone;
            if (notificationManager.getNotificationChannel(channelId) != null) return;
            NotificationChannel channel = new NotificationChannel(
                channelId, "Pemberitahuan reservasi - " + toneLabel(tone), NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Nada notifikasi aplikasi Reservasi Sawah.");
            channel.setShowBadge(true);
            channel.enableVibration(true);
            if ("silent".equals(tone)) {
                channel.setSound(null, null);
            } else {
                Uri sound = "alarm".equals(tone)
                    ? android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                    : ("ringtone".equals(tone)
                        ? android.provider.Settings.System.DEFAULT_RINGTONE_URI
                        : android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                channel.setSound(sound, android.media.AudioAttributes.DEFAULT);
            }
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String toneLabel(String tone) {
        if ("alarm".equals(tone)) return "Alarm";
        if ("ringtone".equals(tone)) return "Telepon";
        if ("silent".equals(tone)) return "Tanpa suara";
        return "Standar";
    }

    private void chooseTone() {
        final String[] labels = {"Standar", "Alarm", "Nada telepon", "Tanpa suara"};
        final String[] values = {"default", "alarm", "ringtone", "silent"};
        int selected = java.util.Arrays.asList(values).indexOf(getSelectedTone());
        new AlertDialog.Builder(this)
            .setTitle("Pilih Nada Notifikasi")
            .setSingleChoiceItems(labels, Math.max(0, selected), (dialog, which) -> {
                String tone = values[which];
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_TONE, tone).apply();
                createEventsChannel(tone);
                dialog.dismiss();
                new AlertDialog.Builder(this)
                    .setMessage("Nada notifikasi diubah menjadi: " + labels[which] + ". Jika nada pernah diubah di setelan Android, periksa setelan kanal notifikasi aplikasi.")
                    .setPositiveButton("OK", null).show();
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private boolean canPostNotifications() {
        return Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private class AndroidNotificationsBridge {
        @JavascriptInterface
        public boolean isAvailable() { return true; }

        @JavascriptInterface
        public void show(final String title, final String body) {
            runOnUiThread(() -> {
                if (notificationManager == null || !canPostNotifications()) return;
                Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(MainActivity.this, EVENTS_CHANNEL_PREFIX + getSelectedTone())
                    : new Notification.Builder(MainActivity.this);
                Notification notification = builder
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title == null || title.isEmpty() ? "Pemberitahuan Reservasi" : title)
                    .setContentText(body == null ? "" : body)
                    .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                    .setCategory(Notification.CATEGORY_EVENT)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build();
                notificationManager.notify((int) (System.currentTimeMillis() & 0x7fffffff), notification);
            });
        }
    }

    private class BadgeBridge {
        @JavascriptInterface
        public void setPendingCount(final int count) {
            runOnUiThread(() -> {
                if (notificationManager == null || !canPostNotifications()) return;
                if (count <= 0) {
                    notificationManager.cancel(BADGE_NOTIFICATION_ID);
                    return;
                }
                Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Notification.Builder(MainActivity.this, CHANNEL_ID)
                    : new Notification.Builder(MainActivity.this);
                Notification notification = builder
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Reservasi menunggu")
                    .setContentText("Ada reservasi yang menunggu persetujuan admin.")
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setSound(null)
                    .build();
                notificationManager.notify(BADGE_NOTIFICATION_ID, notification);
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}