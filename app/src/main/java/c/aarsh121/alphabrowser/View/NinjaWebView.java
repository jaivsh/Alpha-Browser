package c.aarsh121.alphabrowser.View;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;

import c.aarsh121.alphabrowser.Browser.AdBlock;
import c.aarsh121.alphabrowser.Browser.AlbumController;
import c.aarsh121.alphabrowser.Browser.BrowserController;
import c.aarsh121.alphabrowser.Browser.Cookie;
import c.aarsh121.alphabrowser.Browser.Javascript;
import c.aarsh121.alphabrowser.Browser.NinjaClickHandler;
import c.aarsh121.alphabrowser.Browser.NinjaDownloadListener;
import c.aarsh121.alphabrowser.Browser.NinjaGestureListener;
import c.aarsh121.alphabrowser.Browser.NinjaWebChromeClient;
import c.aarsh121.alphabrowser.Browser.NinjaWebViewClient;
import c.aarsh121.alphabrowser.R;
import c.aarsh121.alphabrowser.Unit.BrowserUnit;
import c.aarsh121.alphabrowser.Unit.HelperUnit;
import c.aarsh121.alphabrowser.Unit.IntentUnit;
import c.aarsh121.alphabrowser.Unit.ViewUnit;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;


    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {

        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;
    private int dimen144dp;
    private int dimen108dp;
    private int animTime;

    private Album album;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;
    private NinjaClickHandler clickHandler;
    private GestureDetector gestureDetector;

    private AdBlock adBlock;
    public AdBlock getAdBlock() {
        return adBlock;
    }

    private Javascript javaHosts;
    private Cookie cookieHosts;
    public Cookie getCookieHosts() {
        return cookieHosts;
    }

    private SharedPreferences sp;
    private WebSettings webSettings;

    private boolean foreground;
    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;
    public BrowserController getBrowserController() {
        return browserController;
    }
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an Activity

        this.context = context;
        this.dimen144dp = getResources().getDimensionPixelSize(R.dimen.layout_width_144dp);
        this.dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        this.animTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.foreground = false;

        this.adBlock = new AdBlock(this.context);
        this.javaHosts = new Javascript(this.context);
        this.cookieHosts = new Cookie(this.context);
        this.album = new Album(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        this.clickHandler = new NinjaClickHandler(this);
        this.gestureDetector = new GestureDetector(context, new NinjaGestureListener(this));

        initWebView();
        initWebSettings();
        initPreferences();
        initAlbum();
    }

    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener(new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    private synchronized void initWebSettings() {
        webSettings = getSettings();

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);

        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);

        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);

    }

    public synchronized void initPreferences() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        webViewClient.enableAdBlock(sp.getBoolean(context.getString(R.string.sp_ad_block), true));

        webSettings = getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(sp.getBoolean(("sp_remote"), true));
            webSettings.setAllowUniversalAccessFromFileURLs(sp.getBoolean(("sp_remote"), true));
        }


        webSettings.setBlockNetworkImage(!sp.getBoolean(context.getString(R.string.sp_images), true));
        webSettings.setJavaScriptEnabled(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(context.getString(R.string.sp_javascript), true));

        if (sp.getBoolean(("sp_remote"), true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            }
        }

        if (sp.getBoolean(context.getString(R.string.sp_location), true)) {
            if (Build.VERSION.SDK_INT >= 23) {
                int hasACCESS_FINE_LOCATION = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                    Activity activity = (Activity) context;
                    HelperUnit.grantPermissionsLoc(activity);
                } else {
                    webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), true));
                }
            } else {
                webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), true));
            }
        }

        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(sp.getBoolean(context.getString(R.string.sp_cookies), true));
    }

    private synchronized void initAlbum() {
        album.setAlbumCover(null);
        album.setAlbumTitle(context.getString(R.string.album_untitled));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders () {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        if (sp.getBoolean(context.getString(R.string.sp_savedata), false)){
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public synchronized void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            NinjaToast.show(context, R.string.toast_load_error);
            return;
        }

        if (!url.contains("://")) {
            url = BrowserUnit.queryWrapper(context, url.trim());
        }

        if (url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)) {
            Intent intent = IntentUnit.getEmailIntent(MailTo.parse(url));
            context.startActivity(intent);
            reload();
            return;

        } else if (url.startsWith(BrowserUnit.URL_SCHEME_INTENT)) {
            Intent intent;
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                context.startActivity(intent);
            } catch (URISyntaxException u) {
                Log.w("Browser", "Error parsing URL");
            }
            return;
        }

        if (!sp.getBoolean(context.getString(R.string.sp_javascript), true)) {

            if (javaHosts.isWhite(url)) {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webSettings.setJavaScriptEnabled(true);
            } else {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
                webSettings.setJavaScriptEnabled(false);
            }
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {

            if (cookieHosts.isWhite(url)) {
                CookieManager manager = CookieManager.getInstance();
                manager.getCookie(url);
                manager.setAcceptCookie(true);
            } else {
                CookieManager manager = CookieManager.getInstance();
                manager.setAcceptCookie(false);
            }
        }

        webViewClient.updateWhite(adBlock.isWhite(url));
        super.loadUrl(url, getRequestHeaders());

        if (browserController != null && foreground) {
            browserController.updateBookmarks();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void reload() {
        webViewClient.updateWhite(adBlock.isWhite(getUrl()));

        if (!sp.getBoolean(context.getString(R.string.sp_javascript), true)) {

            if (javaHosts.isWhite(getUrl())) {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webSettings.setJavaScriptEnabled(true);
            } else {
                webSettings = getSettings();
                webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
                webSettings.setJavaScriptEnabled(false);
            }
        }

        if (!sp.getBoolean(context.getString(R.string.sp_cookies), true)) {

            if (cookieHosts.isWhite(getUrl())) {
                CookieManager manager = CookieManager.getInstance();
                manager.getCookie(getUrl());
                manager.setAcceptCookie(true);
            } else {
                CookieManager manager = CookieManager.getInstance();
                manager.setAcceptCookie(false);
            }
        }

        super.reload();
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    @Override
    public void setAlbumCover(Bitmap bitmap) {
        album.setAlbumCover(bitmap);
    }

    @Override
    public String getAlbumTitle() {
        return album.getAlbumTitle();
    }

    @Override
    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void update(int progress) {
        if (foreground) {
            browserController.updateProgress(progress);
        }

        setAlbumCover(ViewUnit.capture(this, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
        if (isLoadFinish()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAlbumCover(ViewUnit.capture(NinjaWebView.this, dimen144dp, dimen108dp, Bitmap.Config.RGB_565));
                }
            }, animTime);

            if (prepareRecord()) {
                browserController.updateAutoComplete();
            }
        }
    }

    public synchronized void update(String title, String url) {
        album.setAlbumTitle(title);
        if (foreground) {
            browserController.updateBookmarks();
            browserController.updateInputBox(url);
        }
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        if (click != null) {
            click.setTarget(clickHandler);
        }
        requestFocusNodeHref(click);
    }

    private boolean prepareRecord() {
        String title = getTitle();
        String url = getUrl();

        return !(title == null
                || title.isEmpty()
                || url == null
                || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT));
    }
}
