package top.xhmsmd.jssafeinjectdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

/**
 * Main
 * Created by Pollex on 2017/3/27.
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private SafeWebView webView;
    private JSInject jsInject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (SafeWebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        jsInject = new JSInject();
        webView.addJavascriptInterface(jsInject, "jsSafeInject");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/html/JSInjectTest.html");
    }


    class JSInject {

        @JavascriptInterface
        public void test(String msg, String msg1) {
            Log.d(TAG, "test is running,msg = " + msg + ", msg1 = " + msg1);
        }
    }
}
