package com.dc.fitnessauthentication;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.dc.fitnessauthentication.databinding.HealthDeviceAuthActivityBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by HB on 31/5/18.
 */
public abstract class DeviceAuthenticateActivity extends AppCompatActivity {

    public CompositeDisposable compositeDisposable;
    public String key, secret, callbackUrl;
    public ArrayList<String> scope;
    protected HealthDeviceAuthActivityBinding binding;
    public static final int DAYS_HISTORY = 45;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        compositeDisposable = new CompositeDisposable();

        binding = DataBindingUtil.setContentView(this, R.layout.health_device_auth_activity);
        binding.getRoot().setVisibility(View.GONE);


        key = getIntent().getStringExtra("key");
        secret = getIntent().getStringExtra("secret");
        callbackUrl = getIntent().getStringExtra("callbackUrl");
        scope = getIntent().getStringArrayListExtra("scope");

    }

    public static Intent getLaunchIntent(Activity activity, String key, String secret, String callback,
                                         ArrayList<String> scope, Class className) {
        Intent intent = new Intent(activity, className);
        intent.putExtra("key", key);
        intent.putExtra("secret", secret);
        intent.putExtra("callbackUrl", callback);
        intent.putStringArrayListExtra("scope", scope);
        return intent;
    }

    public void sendFailResult(String message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void sendResult(String oauthToken, String oauthSecret, String accessToken, String refreshToken, String userId, int expiresIn) {
        Intent intent = new Intent();
        intent.putExtra("oauthToken", oauthToken);
        intent.putExtra("oauthSecret", oauthSecret);
        intent.putExtra("accessToken", accessToken);
        intent.putExtra("refreshToken", refreshToken);
        intent.putExtra("userId", userId);
        intent.putExtra("expiresIn", expiresIn);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void showProgress(boolean show) {
        /*binding.pbProgress.setVisibility(show ? View.VISIBLE : View.GONE);*/
    }

    protected final void resetTimeInCalendar(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    protected final long toUtc(long miliis) {
        int offset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        return miliis + offset;
    }

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public final String getFormattedDate(long miliis) {
        return simpleDateFormat.format(new Date(miliis));
    }

    public void addDisposable(Disposable disposable) {
        compositeDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    public final <T> ObservableTransformer<T, T> applySchedulers() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                return upstream.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }
}
