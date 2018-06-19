package com.dc.fitnessauthentication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.dc.fitnessauthentication.customapi.StepCountReader;
import com.dc.fitnessauthentication.model.StepCount;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SamsungHealthActivity extends DeviceAuthenticateActivity {

    public static String TAG = "Testing";
    private HealthDataStore mStore;
    private StepCountReader mReporter;

    public static Intent getLaunchIntent(Context context) {
        return new Intent(context, SamsungHealthActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showProgress(false);
        initSamsungHealth();
    }

    private void initSamsungHealth() {
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(this, mConnectionListener);

        // Request the connection to the health data store
        mStore.connectService();
        mReporter = new StepCountReader(mStore, mStepCountObserver);
    }


    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            if (isPermissionAcquired()) {
                onPermissionGranted();
            } else {
                requestPermission();
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "onConnectionFailed");
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");
            if (!isFinishing()) {
                mStore.connectService();
            }
        }
    };

    @SuppressLint("StaticFieldLeak")
    private void onPermissionGranted() {
        // Get the daily step count of a particular day and display it
        new AsyncTask<Void, Void, ArrayList<StepCount>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgress(true);
            }

            @Override
            protected ArrayList<StepCount> doInBackground(Void... voids) {
                Calendar calendar = Calendar.getInstance();
                resetTimeInCalendar(calendar);

                final ArrayList<StepCount> stepCounts = new ArrayList<>();
                for (int i = 0; i < DAYS_HISTORY; i++) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1);

                    long startTime = toUtc(calendar.getTimeInMillis());

                    int count = mReporter.readStepCount(startTime, startTime + StepCountReader.ONE_DAY);
                    stepCounts.add(new StepCount(getFormattedDate(startTime), String.valueOf(count)));
                    /*Toast.makeText(baseApplication, "" + count, Toast.LENGTH_SHORT).show();*/
                }
                return stepCounts;
            }

            @Override
            protected void onPostExecute(ArrayList<StepCount> stringIntegerHashMap) {
                showProgress(false);
                super.onPostExecute(stringIntegerHashMap);
                Log.d("stepCounts", stringIntegerHashMap.toString());
                Intent intent = new Intent();
                intent.putExtra("stepCounts", stringIntegerHashMap);
                setResult(RESULT_OK, intent);
                finish();
            }
        }.execute();

    }


    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Check whether the permissions that this application needs are acquired
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(
                    generatePermissionKeySet());
            return !resultMap.values().contains(Boolean.FALSE);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Show user permission UI for allowing user to change options
            pmsManager.requestPermissions(generatePermissionKeySet(), SamsungHealthActivity.this)
                    .setResultListener(mPermissionListener);
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private Set<HealthPermissionManager.PermissionKey> generatePermissionKeySet() {
        Set<HealthPermissionManager.PermissionKey> pmsKeySet = new HashSet<>();
        pmsKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        pmsKeySet.add(new HealthPermissionManager.PermissionKey(StepCountReader.STEP_SUMMARY_DATA_TYPE_NAME, HealthPermissionManager.PermissionType.READ));
        return pmsKeySet;
    }


    private final HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult>() {

                @Override
                public void onResult(HealthPermissionManager.PermissionResult result) {
                    Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();
                    // Show a permission alarm and clear step count if permissions are not acquired
                    if (resultMap.values().contains(Boolean.FALSE)) {
                        updateStepCountView("");
                        showPermissionAlarmDialog();
                    } else {
                        onPermissionGranted();
                    }
                }
            };

    private void showPermissionAlarmDialog() {
        if (isFinishing()) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(SamsungHealthActivity.this);
        alert.setTitle("Notice")
                .setMessage("Permission Required")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showConnectionFailureDialog(final HealthConnectionErrorResult error) {
        if (isFinishing()) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    alert.setMessage("Installation Required");
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    alert.setMessage("Upgrade Required");
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    alert.setMessage("Request disable");
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    alert.setMessage("Request agree?");
                    break;
                default:
                    alert.setMessage("Request available");
                    break;
            }
        } else {
            alert.setMessage("Connection not available");
        }

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (error.hasResolution()) {
                    error.resolve(SamsungHealthActivity.this);
                    finish();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendFailResult("Samsung Health Connect Error");
            }
        });
        alert.setCancelable(false);
        alert.show();
    }

    private final StepCountReader.StepCountObserver mStepCountObserver = new StepCountReader.StepCountObserver() {
        @Override
        public void onChanged(int count) {
            updateStepCountView(String.valueOf(count));
        }

        @Override
        public void onBinningDataChanged(List<StepCountReader.StepBinningData> stepBinningDataList) {
            updateBinningChartView(stepBinningDataList);
        }
    };

    private void updateBinningChartView(List<StepCountReader.StepBinningData> stepBinningDataList) {
        // the following code will be replaced with chart drawing code
        for (StepCountReader.StepBinningData data : stepBinningDataList) {
            Log.d(TAG, "TIME : " + data.time + "  COUNT : " + data.count);
        }
    }

    private void updateStepCountView(final String count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*Toast.makeText(SamsungHealthActivity.this, "Count:" + count, Toast.LENGTH_SHORT).show();*/
            }
        });
    }
}
