package jlab.website;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;
import android.widget.Button;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.graphics.Bitmap;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import jlab.website.view.CustomWebView;
import android.support.v4.util.ArrayMap;
import android.view.SoundEffectConstants;
import android.support.v7.app.AppCompatActivity;
import jlab.website.view.CustomSwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import jlab.website.view.VideoEnabledWebChromeClient;


public class WebSiteActivity extends AppCompatActivity implements CustomSwipeRefreshLayout.CanChildScrollUpCallback{

    private CustomWebView wv;
    private String URLS_LOADED_KEY = "URLS_LOADED_KEY";
    private ArrayList<String> urlsLoaded = new ArrayList<>();
    private CustomSwipeRefreshLayout srlRefresh;
    private RelativeLayout llOffline;
    private Button btRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_te_veo);
        wv = (CustomWebView) findViewById(R.id.wvWebSite);
        WebSettings webSettings = wv.getSettings();
        webSettings.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT > 18)
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDisplayZoomControls(false);
        btRefresh = (Button) findViewById(R.id.btRefresh);
        btRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCurrentPage();
            }
        });
        llOffline = (RelativeLayout) findViewById(R.id.rlOffline);
        srlRefresh = (CustomSwipeRefreshLayout) findViewById(R.id.srlRefresh);
        srlRefresh.setProgressViewOffset(false, 80, 120);
        srlRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark));
        srlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadCurrentPage();
            }
        });
        srlRefresh.setCanChildScrollUpCallback(this);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String url2 = getString(R.string.url_root);
                String [] prefixs = getResources().getStringArray(R.array.prefix_url_not_linked);
                if (url != null && (url.startsWith(url2) && !startWithAnyPrefix(prefixs, url))) {
                    urlsLoaded.add(url);
                    view.playSoundEffect(SoundEffectConstants.CLICK);
                    return false;
                } else
                    return true;
            }

            private boolean startWithAnyPrefix(String [] prefixs, String str) {
                for (String pref : prefixs)
                    if(str.startsWith(pref))
                        return true;
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                llOffline.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                llOffline.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //Delete ads
                view.loadUrl("javascript:(function() {\n" +
                        "  var x = document.getElementsByClassName(\"adsbygoogle\");\n" +
                        "  var i = 0;\n" +
                        "  for(; i < x.length; i++) {\n" +
                        "     x[i].style.display = 'none';\n" +
                        "  }})()");
            }
        });
        ViewGroup videoLayout = (ViewGroup) findViewById(R.id.videoLayout); // Your own view, read class comments
        View loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null); // Your own view, read class comments

        VideoEnabledWebChromeClient chromeClient = new VideoEnabledWebChromeClient(findViewById(R.id.rlWebViewWrapper),
                videoLayout, loadingView, wv){
            public void onProgressChanged(WebView view, int progress)
            {
                if(progress < 100 && !srlRefresh.isRefreshing())
                    srlRefresh.setRefreshing(true);
                if(progress == 100)
                    srlRefresh.setRefreshing(false);
                super.onProgressChanged(view, progress);
            }
        };
        chromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback()
        {
            @Override
            public void toggledFullscreen(boolean fullscreen)
            {
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                if (fullscreen)
                {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
                else
                {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }

            }
        });
        wv.setWebChromeClient(chromeClient);
        if (savedInstanceState != null)
            urlsLoaded = savedInstanceState.getStringArrayList(URLS_LOADED_KEY);
        else
            urlsLoaded.add(getString(R.string.url_initial));
        loadCurrentPage();
    }

    @Override
    public void onBackPressed() {
        if (urlsLoaded.size() > 1) {
            urlsLoaded.remove(urlsLoaded.size() - 1);
            loadCurrentPage();
        }
        else
            super.onBackPressed();
    }

    private void loadCurrentPage () {
        ArrayMap<String, String> extraHeaders = new ArrayMap<>();
//        extraHeaders.put("Accept-Language", Locale.getDefault().toLanguageTag());
        wv.loadUrl(urlsLoaded.get(urlsLoaded.size() - 1), extraHeaders);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(URLS_LOADED_KEY, urlsLoaded);
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return wv.getScrollY() > 0;
    }
}
