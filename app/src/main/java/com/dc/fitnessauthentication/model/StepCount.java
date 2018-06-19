package com.dc.fitnessauthentication.model;

import java.io.Serializable;

/**
 * Created by HB on 4/6/18.
 */
public class StepCount implements Serializable {


    private String date;
    private String count;

    private boolean isSelected;

    public StepCount(String date, String count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return date + ":" + count;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
