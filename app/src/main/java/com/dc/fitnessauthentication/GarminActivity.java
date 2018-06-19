package com.dc.fitnessauthentication;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.dc.fitnessauthentication.customapi.GarminAPI;
import com.dc.fitnessauthentication.model.OAuth1RequestTokenUrl;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;


/**
 * Created by HB on 30/5/18.
 */
public class GarminActivity extends DeviceAuthenticateActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGarmin();
    }

    // Step 1
    @SuppressLint("StaticFieldLeak")
    private void initGarmin() {
        getAuthorizationUrlObservable()
                .compose(this.<OAuth1RequestTokenUrl>applySchedulers())
                .subscribe(new Observer<OAuth1RequestTokenUrl>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onNext(OAuth1RequestTokenUrl oAuth1RequestTokenUrl) {
                        Log.d("Auth Url", oAuth1RequestTokenUrl.url);
                        openUrl(oAuth1RequestTokenUrl);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public Observable<OAuth1RequestTokenUrl> getAuthorizationUrlObservable() {
        return Observable.defer(new Callable<ObservableSource<OAuth1RequestTokenUrl>>() {
            @Override
            public ObservableSource<OAuth1RequestTokenUrl> call() throws InterruptedException, ExecutionException, IOException {
                return Observable.just(getAuthorizationUrl());
            }
        });
    }

    public OAuth1RequestTokenUrl getAuthorizationUrl() throws InterruptedException, ExecutionException, IOException {
        OAuth1RequestTokenUrl oAuth1RequestTokenUrl = new OAuth1RequestTokenUrl();
        OAuth1RequestToken requestToken = getService().getRequestToken();
        oAuth1RequestTokenUrl.requestToken = requestToken;
        oAuth1RequestTokenUrl.url = getService().getAuthorizationUrl(requestToken);
        return oAuth1RequestTokenUrl;
    }

    // Step 2
    public void openUrl(final OAuth1RequestTokenUrl oAuth1RequestTokenUrl) {
        binding.getRoot().setVisibility(View.VISIBLE);
        WebView webView = binding.webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showProgress(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showProgress(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains(callbackUrl)) {
                    onGarminAuthTokenCallback(oAuth1RequestTokenUrl, url);
                    return false;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(oAuth1RequestTokenUrl.url);
    }

    // Step 3
    private void onGarminAuthTokenCallback(final OAuth1RequestTokenUrl oAuth1RequestTokenUrl, final String url) {
        binding.webView.setVisibility(View.GONE);
        Uri uri = Uri.parse(url);
        /*String authToken = uri.getQueryParameter("oauth_token");*/
        oAuth1RequestTokenUrl.oAuthVerifier = uri.getQueryParameter("oauth_verifier");
        getAccessTokenObservable(oAuth1RequestTokenUrl)
                .compose(this.<OAuth1AccessToken>applySchedulers())
                .subscribe(new Observer<OAuth1AccessToken>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onNext(OAuth1AccessToken oAuth1AccessToken) {
                        onGarminAccessToken(oAuth1AccessToken);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof OAuthException) {
                            sendFailResult("Garmin Connect API Server Error: Unauthorized");
                        } else {
                            sendFailResult("Something went Wrong.");
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public Observable<OAuth1AccessToken> getAccessTokenObservable(OAuth1RequestTokenUrl oAuth1RequestTokenUrl) {
        return Observable.just(oAuth1RequestTokenUrl).flatMap(new Function<OAuth1RequestTokenUrl, ObservableSource<OAuth1AccessToken>>() {
            @Override
            public ObservableSource<OAuth1AccessToken> apply(OAuth1RequestTokenUrl oAuth1RequestTokenUrl) throws Exception {
                return Observable.just(getService().getAccessToken(oAuth1RequestTokenUrl.requestToken, oAuth1RequestTokenUrl.oAuthVerifier));
            }
        });
    }


    // Result
    private void onGarminAccessToken(final OAuth1AccessToken accessToken) {
        Log.d("Access Token", accessToken.getToken());
        sendResult(accessToken.getToken(), accessToken.getTokenSecret(), "", "", "", -1);
    }

    private OAuth10aService getService() {
        return new ServiceBuilder(key)
                .scope(TextUtils.join(" ", scope))
                .apiSecret(secret)
                .callback(callbackUrl)
                .build(new GarminAPI());
    }
}
