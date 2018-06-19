package com.dc.fitnessauthentication;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.dc.fitnessauthentication.databinding.ActivityMainBinding;
import com.dc.fitnessauthentication.model.Keys;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ArrayList<String> scopes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        scopes.add("profile");
        scopes.add("activity");
        scopes.add("weight");
        scopes.add("heartrate");

        binding.btnFitbit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(DeviceAuthenticateActivity.getLaunchIntent(MainActivity.this,
                        Keys.Fitbit_KEY, Keys.FItbit_SECRET, Keys.FITBIT_CALLBACK_URL,
                        scopes, FitbitActivity.class), 101);
            }
        });

        binding.btnGarmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(DeviceAuthenticateActivity.getLaunchIntent(MainActivity.this,
                        Keys.GARMIN_KEY, Keys.GARMIN_SECRET, Keys.GARMIN_CALLBACK,
                        scopes, GarminActivity.class), 102);
            }
        });

        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        binding.btnSamsung.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 101) {
                String accessToken = data.getStringExtra("accessToken");
                Toast.makeText(this, accessToken, Toast.LENGTH_SHORT).show();
            }
            if (requestCode == 102) {
                String oAuthToken = data.getStringExtra("oauthToken");
                String tokenSecret = data.getStringExtra("oauthSecret");
                Toast.makeText(this, oAuthToken, Toast.LENGTH_SHORT).show();
                Toast.makeText(this, tokenSecret, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
