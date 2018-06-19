/**
 * Copyright (C) 2014 Samsung Electronics Co., Ltd. All rights reserved.
 * <p>
 * Mobile Communication Division,
 * Digital Media & Communications Business, Samsung Electronics Co., Ltd.
 * <p>
 * This software and its documentation are confidential and proprietary
 * information of Samsung Electronics Co., Ltd.  No part of the software and
 * documents may be copied, reproduced, transmitted, translated, or reduced to
 * any electronic medium or machine-readable form without the prior written
 * consent of Samsung Electronics.
 * <p>
 * Samsung Electronics makes no representations with respect to the contents,
 * and assumes no responsibility for any errors that might appear in the
 * software and documents. This publication and the contents hereof are subject
 * to change without notice.
 */

package com.dc.fitnessauthentication.customapi;

import android.os.Handler;
import android.util.Log;

import com.dc.fitnessauthentication.SamsungHealthActivity;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.AggregateRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.AggregateRequest.AggregateFunction;
import com.samsung.android.sdk.healthdata.HealthDataResolver.SortOrder;
import com.samsung.android.sdk.healthdata.HealthDataStore;

import java.util.Iterator;
import java.util.List;


public class StepCountReader {

    public static final String STEP_SUMMARY_DATA_TYPE_NAME = "com.samsung.shealth.step_daily_trend";

    /*public static final long TODAY_START_UTC_TIME;*/
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private static final String PROPERTY_TIME = "day_time";
    private static final String PROPERTY_COUNT = "count";
    private static final String PROPERTY_BINNING_DATA = "binning_data";
    private static final String ALIAS_TOTAL_COUNT = "count";
    private static final String ALIAS_DEVICE_UUID = "deviceuuid";
    private static final String ALIAS_BINNING_TIME = "binning_time";

    private final HealthDataResolver mResolver;
    private final StepCountObserver mObserver;

    public interface StepCountCallback {
        public void onStepCount(int count, long startTime, long endTime);
    }

    private StepCountCallback stepCountCallback;


    public StepCountReader(HealthDataStore store, StepCountObserver observer) {
        mResolver = new HealthDataResolver(store, new Handler());
        mObserver = observer;
    }

    // Get the daily total step count of a specified day
    /*public void requestDailyStepCount(long startTime, long endTime) {
        readStepCount(startTime, endTime);
        *//*if (startTime >= TODAY_START_UTC_TIME) {
            // Get today step count

        } else {
            // Get historical step count
            readStepDailyTrend(startTime);
        }*//*
    }*/

    public int readStepCount(final long startTime, final long endTime) {

        // Get sum of step counts by device
        AggregateRequest request = new AggregateRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .addFunction(AggregateFunction.SUM, HealthConstants.StepCount.COUNT, ALIAS_TOTAL_COUNT)
                .addGroup(HealthConstants.StepCount.DEVICE_UUID, ALIAS_DEVICE_UUID)
                .setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                        startTime, endTime)
                .setSort(ALIAS_TOTAL_COUNT, SortOrder.DESC)
                .build();

        try {

            HealthDataResolver.AggregateResult result = mResolver.aggregate(request).await();
            int totalCount = 0;
            try {
                Iterator<HealthData> iterator = result.iterator();
                if (iterator.hasNext()) {
                    HealthData data = iterator.next();
                    totalCount = data.getInt(ALIAS_TOTAL_COUNT);
                }
            } finally {
                result.close();
            }

            return totalCount;

        } catch (Exception e) {
            Log.e(SamsungHealthActivity.TAG, "Getting step count fails.", e);
        }

        return 0;
    }


    public static class StepBinningData {
        public String time;
        public final int count;

        public StepBinningData(String time, int count) {
            this.time = time;
            this.count = count;
        }
    }

    public interface StepCountObserver {
        void onChanged(int count);

        void onBinningDataChanged(List<StepBinningData> binningCountList);
    }



    /*static {
        TODAY_START_UTC_TIME = getTodayStartUtcTime();
    }

    private static long getTodayStartUtcTime() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Log.d(SamsungHealthActivity.TAG, "Today : " + today.getTimeInMillis());

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }*/

   /* private void readStepCountBinning(final long startTime, String deviceUuid) {

        Filter filter = Filter.eq(HealthConstants.StepCount.DEVICE_UUID, deviceUuid);

        // Get 10 minute binning data of a particular device
        AggregateRequest request = new AggregateRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .addFunction(AggregateFunction.SUM, HealthConstants.StepCount.COUNT, ALIAS_TOTAL_COUNT)
                .setTimeGroup(TimeGroupUnit.MINUTELY, 10, HealthConstants.StepCount.START_TIME,
                        HealthConstants.StepCount.TIME_OFFSET, ALIAS_BINNING_TIME)
                .setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                        startTime, startTime + ONE_DAY)
                .setFilter(filter)
                .setSort(ALIAS_BINNING_TIME, SortOrder.ASC)
                .build();

        try {
            mResolver.aggregate(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.AggregateResult>() {
                @Override
                public void onResult(HealthDataResolver.AggregateResult result) {
                    List<StepBinningData> binningCountArray = new ArrayList<>();

                    try {
                        for (HealthData data : result) {
                            String binningTime = data.getString(ALIAS_BINNING_TIME);
                            int binningCount = data.getInt(ALIAS_TOTAL_COUNT);

                            if (binningTime != null) {
                                binningCountArray.add(new StepBinningData(binningTime.split(" ")[1], binningCount));
                            }
                        }

                        if (mObserver != null) {
                            mObserver.onBinningDataChanged(binningCountArray);
                        }

                    } finally {
                        result.close();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(SamsungHealthActivity.TAG, "Getting step binning data fails.", e);
        }
    }*/

   /* private void readStepDailyTrend(final long startTime) {

        Filter filter = Filter.and(Filter.eq(PROPERTY_TIME, startTime),
                // filtering source type "combined(-2)"
                Filter.eq("source_type", -2));

        ReadRequest request = new ReadRequest.Builder()
                .setDataType(STEP_SUMMARY_DATA_TYPE_NAME)
                .setProperties(new String[]{PROPERTY_COUNT, PROPERTY_BINNING_DATA})
                .setFilter(filter)
                .build();

        try {
            mResolver.read(request).setResultListener(
                    new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                        @Override
                        public void onResult(HealthDataResolver.ReadResult result) {
                            int totalCount = 0;
                            List<StepBinningData> binningDataList = Collections.emptyList();

                            try {
                                Iterator<HealthData> iterator = result.iterator();
                                if (iterator.hasNext()) {
                                    HealthData data = iterator.next();
                                    totalCount = data.getInt(PROPERTY_COUNT);
                                    byte[] binningData = data.getBlob(PROPERTY_BINNING_DATA);
                                    binningDataList = getBinningData(binningData);
                                }
                            } finally {
                                result.close();
                            }

                            if (mObserver != null) {
                                mObserver.onChanged(totalCount);
                                mObserver.onBinningDataChanged(binningDataList);
                            }

                        }
                    }
            );

        } catch (Exception e) {
            Log.e(SamsungHealthActivity.TAG, "Getting daily step trend fails.", e);
        }
    }*/



   /* private static List<StepBinningData> getBinningData(byte[] zip) {
        // decompress ZIP
        List<StepBinningData> binningDataList = HealthDataUtil.getStructuredDataList(zip, StepBinningData.class);
        for (int i = binningDataList.size() - 1; i >= 0; i--) {
            if (binningDataList.get(i).count == 0) {
                binningDataList.remove(i);
            } else {
                binningDataList.get(i).time = String.format(Locale.US, "%02d:%02d", i / 6, (i % 6) * 10);
            }
        }

        return binningDataList;
    }
*/

}
