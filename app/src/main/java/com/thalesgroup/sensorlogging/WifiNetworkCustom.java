package com.thalesgroup.sensorlogging;

import android.net.wifi.ScanResult;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
 * Created by thales on 01/08/2018.
 */

/**
 * class represents a wifi network with SSID and BSSID
 */
public class WifiNetworkCustom extends RealmObject {

    private String SSID;
    private String BSSID;
    @PrimaryKey
    private long id;

    public WifiNetworkCustom(ScanResult scanResult) {
        this.SSID = scanResult.SSID;
        this.BSSID = scanResult.BSSID;
    }

    public WifiNetworkCustom() {
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    @Override
    public String toString() {
        return "WifiNetworkCustom{" +
                "SSID='" + SSID + '\'' +
                ", BSSID='" + BSSID + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WifiNetworkCustom that = (WifiNetworkCustom) o;

        if (SSID != null ? !SSID.equals(that.SSID) : that.SSID != null) return false;
        return BSSID != null ? BSSID.equals(that.BSSID) : that.BSSID == null;
    }

    @Override
    public int hashCode() {
        int result = SSID != null ? SSID.hashCode() : 0;
        result = 31 * result + (BSSID != null ? BSSID.hashCode() : 0);
        return result;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
