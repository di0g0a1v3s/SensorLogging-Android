package com.thalesgroup.sensorlogging;

/*
 * Created by thales on 01/08/2018.
 */

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * WifiDevice containing IP address in the network, mac address of the device and network SSID
 */
public class WifiDeviceCustom extends RealmObject {
    @PrimaryKey
    private long id;
    private String ip;
    private String mac;
    private String networkSSID;

    public WifiDeviceCustom() {
    }

    public WifiDeviceCustom(String ip, String mac, String networkSSID){
        this.ip = ip;
        this.mac = mac;
        setNetworkSSID(networkSSID);
    }

    @Override
    public String toString() {
        return "WifiDeviceCustom{" +
                "ip='" + ip + '\'' +
                ", mac='" + mac + '\'' +
                ", networkSSID='" + networkSSID + '\'' +
                '}';
    }

    public String getNetworkSSID() {
        return networkSSID;
    }

    public void setNetworkSSID(String network) {
        this.networkSSID = network;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WifiDeviceCustom that = (WifiDeviceCustom) o;

        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (mac != null ? !mac.equals(that.mac) : that.mac != null) return false;
        return networkSSID != null ? networkSSID.equals(that.networkSSID) : that.networkSSID == null;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (mac != null ? mac.hashCode() : 0);
        result = 31 * result + (networkSSID != null ? networkSSID.hashCode() : 0);
        return result;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
