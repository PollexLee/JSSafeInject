package top.xhmsmd.jssafeinjectdemo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by Pollex on 2017/3/27.
 */

public class SafeWebView extends WebView {
    private String JSInjectName;
    private Object JSObject;
    private String jsInjectStr;

    public SafeWebView(Context context) {
        super(context);
        init();
    }

    public SafeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 默认开启JavaScript支持
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 4.3.1及以下，有此漏洞
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            removeJavascriptInterface("searchBoxJavaBridge_");
            removeJavascriptInterface("accessibility");
            removeJavascriptInterface("accessibilityTraversal");
        }


        // 默认设置两个空的client，为了避免使用的地方没有设置，造成Client里面调用的代码不被触发
        setWebViewClient(new WebViewClient());
        setWebChromeClient(new WebChromeClient());
    }

    /**
     * 重写此方法，SDK版本大于等于17的才执行add方法
     *
     * @param object
     * @param name
     */
    @SuppressLint("JavascriptInterface")
    @Override
    public void addJavascriptInterface(Object object, String name) {
//        if (Build.VERSION.SDK_INT >= 17) {
//            super.addJavascriptInterface(object, name);
//        } else {
            JSInjectName = name;
            JSObject = object;
            performJSInjectStr(object, JSInjectName);
//        }
    }

    /**
     * 根据注入的类，生成JavaScript
     * @param object
     */
    private void performJSInjectStr(Object object, String jsInjectName) {
        String js = "javascript:" + jsInjectName + "={};";
        for (Method method : object.getClass().getMethods()) {
            if (null != method) {
                Annotation[] annotations = method.getAnnotations();
                for(Annotation annotation : annotations){
                    String name = annotation.annotationType().getCanonicalName().toLowerCase();
                    if(name.endsWith("javascriptinterface")){
                        // 2017/3/28 生成JS
                        String paramsStr = "";
                        String promptParamsStr = "";
                        for (int i = 1; i <= method.getParameterTypes().length; i++) {
                            paramsStr += "param" + i + ",";
                            promptParamsStr += "param" + i + "+','+";
                        }
                        if (paramsStr.contains(",")) {
                            paramsStr = paramsStr.substring(0, paramsStr.length() - 1);
                            promptParamsStr = promptParamsStr.substring(0, promptParamsStr.length() - 5);
                        }
                        String itemJS = jsInjectName + "." + method.getName() + " = function(" + paramsStr + "){ prompt('"+method.getName()+"'," + promptParamsStr + ");};";
                        js += itemJS;
                    }
                }

            }
        }
        jsInjectStr = js;
    }

    @Override
    public void setWebChromeClient(final WebChromeClient client) {
        WebChromeClient mClient = new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                client.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                client.onReceivedTitle(view, title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                client.onReceivedIcon(view, icon);
            }

            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
                client.onReceivedTouchIconUrl(view, url, precomposed);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                client.onShowCustomView(view, callback);
            }

            @Override
            public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
                client.onShowCustomView(view, requestedOrientation, callback);
            }

            @Override
            public void onHideCustomView() {
                client.onHideCustomView();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return client.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }

            @Override
            public void onRequestFocus(WebView view) {
                client.onRequestFocus(view);
            }

            @Override
            public void onCloseWindow(WebView window) {
                client.onCloseWindow(window);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return client.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return client.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
//                String methodName = "";
                Log.d("SafeWebView","message = " + message + ", defaultValue = " + defaultValue);
                Method[] methods = JSObject.getClass().getMethods();
                for(Method method : methods){
                    if(message.equals(method.getName())){
                        try {
                            Object[] params = defaultValue.split(",");
                            method.invoke(JSObject,params);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }

                }
                result.confirm("");
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                return client.onJsBeforeUnload(view, url, message, result);
            }

            @Override
            public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, WebStorage.QuotaUpdater quotaUpdater) {
                client.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
            }

            @Override
            public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
                client.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                client.onGeolocationPermissionsShowPrompt(origin, callback);
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                client.onGeolocationPermissionsHidePrompt();
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                client.onPermissionRequest(request);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                client.onPermissionRequestCanceled(request);
            }

            @Override
            public boolean onJsTimeout() {
                return client.onJsTimeout();
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                client.onConsoleMessage(message, lineNumber, sourceID);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return client.onConsoleMessage(consoleMessage);
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                return client.getDefaultVideoPoster();
            }

            @Override
            public View getVideoLoadingProgressView() {
                return client.getVideoLoadingProgressView();
            }

            @Override
            public void getVisitedHistory(ValueCallback<String[]> callback) {
                client.getVisitedHistory(callback);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return client.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        };
        super.setWebChromeClient(mClient);
    }

    @Override
    public void setWebViewClient(final WebViewClient client) {
        WebViewClient mClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return client.shouldOverrideUrlLoading(view, url);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return client.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                client.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 执行JS注入
                loadUrl(jsInjectStr);
                client.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                client.onLoadResource(view, url);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                client.onPageCommitVisible(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return client.shouldInterceptRequest(view, url);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return client.shouldInterceptRequest(view, request);
            }

            @Override
            public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
                super.onTooManyRedirects(view, cancelMsg, continueMsg);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                client.onReceivedError(view, errorCode, description, failingUrl);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                client.onReceivedError(view, request, error);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                client.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                client.onFormResubmission(view, dontResend, resend);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                client.doUpdateVisitedHistory(view, url, isReload);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                client.onReceivedSslError(view, handler, error);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                client.onReceivedClientCertRequest(view, request);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                client.onReceivedHttpAuthRequest(view, handler, host, realm);
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                return client.shouldOverrideKeyEvent(view, event);
            }

            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                client.onUnhandledKeyEvent(view, event);
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                client.onScaleChanged(view, oldScale, newScale);
            }

            @Override
            public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
                client.onReceivedLoginRequest(view, realm, account, args);
            }
        };

        super.setWebViewClient(mClient);
    }
}
