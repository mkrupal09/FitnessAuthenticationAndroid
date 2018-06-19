package com.dc.fitnessauthentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dc.fitnessauthentication.model.StepCount;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by HB on 31/5/18.
 */
public class GoogleFitActivity extends DeviceAuthenticateActivity {

    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001;
    private String LOG_TAG = "GoogleFit";

    public static Intent getLaunchIntent(Context context) {
        return new Intent(context, GoogleFitActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGoogleFit();
    }

    private void initGoogleFit() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            accessGoogleFit();
        }
    }

    private void accessGoogleFit() {
        Calendar cal = Calendar.getInstance();
        resetTimeInCalendar(cal);
        long endTime = toUtc(cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_MONTH, -DAYS_HISTORY);
        long startTime = toUtc(cal.getTimeInMillis());

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);


        if (account != null) {
            Fitness.getHistoryClient(this, account)
                    .readData(readRequest)
                    .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                        @Override
                        public void onSuccess(DataReadResponse o) {
                            ArrayList<StepCount> stepCounts = new ArrayList<>();
                            Intent intent = new Intent();
                            for (Bucket bucket : o.getBuckets()) {
                                int count = 0;
                                List<DataSet> dataSets = bucket.getDataSets();
                                if (dataSets != null && dataSets.size() > 0) {
                                    for (DataSet dataset : dataSets) {
                                        List<DataPoint> dataPoints = dataset.getDataPoints();
                                        if (dataPoints != null && dataPoints.size() > 0) {
                                            for (DataPoint dataPoint : dataPoints) {
                                                count += dataPoint.getValue(Field.FIELD_STEPS).asInt();
                                            }
                                        }

                                    }
                                }
                                StepCount stepCount = new StepCount(getFormattedDate(bucket.getStartTime(TimeUnit.MILLISECONDS)), String.valueOf(count));
                                stepCounts.add(stepCount);
                            }
                            intent.putExtra("stepCounts", stepCounts);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(LOG_TAG, "onFailure()", e);
                        }
                    });
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                accessGoogleFit();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
