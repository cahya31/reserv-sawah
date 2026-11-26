package id.reservasisawah.admin;

import android.Manifest;
import android.app.Activity;
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
        requestNotificationPermissionIfNeeded();

        webView = new WebView(this);
        webView.addJavascriptInterface(new BadgeBridge(), "AndroidBadge");
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
                        "(function(){if(window.__sawahBadgeTimer)return;" +
                        "function check(){fetch('/api/reservations',{cache:'no-store',credentials:'include'})" +
                        ".then(function(r){if(!r.ok)throw new Error('auth');return r.json();})" +
                        ".then(function(d){var rows=d.reservations||[];" +
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