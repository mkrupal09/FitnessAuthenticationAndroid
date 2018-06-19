package com.dc.fitnessauthentication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.github.scribejava.apis.FitbitApi20;
import com.github.scribejava.apis.fitbit.FitBitOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.exceptions.OAuthException;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

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
public class FitbitActivity extends DeviceAuthenticateActivity {

    private static final String FITBIT_CALLBACK_EXTRA_TEXT = "#_=_";
    private static final String LOG_TAG = "FITBIT";
    private static final String PROFILE_REQUEST = "https://api.fitbit.com/1/user/-/profile.json";
    private OAuth20Service service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onIntent(intent);
    }

    private void onIntent(Intent intent) {
        if (intent.getData() == null) {
            initFitBit();
        } else {
            String callback = intent.getData().toString();
            if (callback.contains(callbackUrl)) {
                onFitBitAuthCode(callback);
            }
        }
    }

    //Step 1
    private void initFitBit() {
        getAuthorizationUrlObservable()
                .compose(this.<String>applySchedulers())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onNext(String s) {
                        openUrl(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        sendFailResult("Something went wrong.");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public Observable<String> getAuthorizationUrlObservable() {
        return Observable.defer(new Callable<ObservableSource<String>>() {
            @Override
            public ObservableSource<String> call() {
                return Observable.just(getService("code").getAuthorizationUrl());
            }
        });
    }


    //Step 2
    private void openUrl(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder().enableUrlBarHiding();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    //Step 3
    private void onFitBitAuthCode(final String url) {
        binding.webView.setVisibility(View.GONE);
        Uri uri = Uri.parse(url);
        final String authToken = uri.getQueryParameter("code").replaceAll(FITBIT_CALLBACK_EXTRA_TEXT, "");
        Log.d(LOG_TAG, "Authorization Code=" + authToken);
        getAccessTokenObservable(authToken)
                .compose(this.<OAuth2AccessToken>applySchedulers())
                .subscribe(new Observer<OAuth2AccessToken>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onNext(OAuth2AccessToken oAuth2AccessToken) {
                        onFitbitAccessToken(oAuth2AccessToken);

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (e instanceof OAuthException) {
                            sendFailResult("Fitbit Connect API Server Error: Unauthorized");
                        } else {
                            sendFailResult("Something went Wrong.");
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private Observable<OAuth2AccessToken> getAccessTokenObservable(String authToken) {
        return Observable.just(authToken)
                .flatMap(new Function<String, ObservableSource<OAuth2AccessToken>>() {
                    @Override
                    public ObservableSource<OAuth2AccessToken> apply(String authToken) throws Exception {
                        return Observable.just(getService(null)
                                .getAccessToken(authToken));
                    }
                });
    }

    //Step 4 : Result
    private void onFitbitAccessToken(OAuth2AccessToken token) {
        String accessToken = token.getAccessToken();
        String refreshedToken = token.getRefreshToken();
        String userId = ((FitBitOAuth2AccessToken) token).getUserId();
        int userExpiresIn = token.getExpiresIn();
        Log.d(LOG_TAG, String.format("Access Token = %s,Refresh Token = %s,UserId = %s,ExpireIn = %d",
                accessToken, refreshedToken, userId, userExpiresIn));
        sendResult("", "", accessToken, refreshedToken, userId, userExpiresIn);
        /*fetchProfile(token);*/
    }

    private OAuth20Service getService(String type) {
        if (service == null) {
            ServiceBuilder serviceBuilder = new ServiceBuilder(key)
                    .scope(TextUtils.join(" ", scope))
                    .apiSecret(secret)
                    .callback(callbackUrl);
            if (!TextUtils.isEmpty(type)) {
                serviceBuilder.responseType(type);
            }
            service = serviceBuilder.build(FitbitApi20.instance());
        }
        return service;
    }

    //Step 5 : Fetch Profile (Optional)
    private void fetchProfile(OAuth2AccessToken token) {
        OAuthRequest profileRequest = new OAuthRequest(Verb.GET, PROFILE_REQUEST);
        OAuth20Service service = getService("");
        service.signRequest(token, profileRequest);
        try {
            Response profileResponse = service.execute(profileRequest);
            System.out.println(profileResponse.getBody());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Step 2

    /*public void openUrl(String url) {
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

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("Url", url);
                if (url.contains(callbackUrl)) {
                    onFitBitAuthCode(url);
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("Url", url);
                if (url.contains(callbackUrl)) {
                    onFitBitAuthCode(url);
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(url);
    }
*/


    /*final OAuth20Service service = getService("code");*/
      /*  new AsyncTask<Object, Object, String>() {

            @Override
            protected String doInBackground(Object... objects) {
                return service.getAuthorizationUrl();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.d(LOG_TAG, "Auth Url=" + s);
                openUrl(s);
            }
        }.execute();*/
}
