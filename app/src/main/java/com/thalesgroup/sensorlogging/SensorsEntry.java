package com.thalesgroup.sensorlogging;


import java.sql.Timestamp;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
  Created by thales on 19/07/2018.
 */

/**
 * Class representing an entry in the database/server containing information about sensors/networks/location/... measured during an interval of time
 */
public class SensorsEntry extends RealmObject {

    @PrimaryKey
    private long id; //identifier (primary key)
    private long beginningTimestamp;
    private long finalTimestamp;
    private String timestamp; //TODO remove
    private int batteryLevel; //absolute value indicates percentage of battery, positive if charging, negative if not charging (measured only in the final timestamp instant)
    private boolean inMotion; //false if the device was practically still (on top of a table for example), true if there was at least some movement during measurement (measurement based on acceleration sensors)
    private boolean moving; //false if device remained roughly in the same spot during measurement, true if there was a significant change in the location (walking, riding the bus...) (measurement based on location sensors)


    private MotionValues motionValues; //field containing information about motion measured with acceleration sensors of the device (measured during the whole interval)


    private boolean onServer; //true if this entry has been uploaded to the server, false if not
    private boolean display; //indicates weather the screen of the device if on (true if on, false if off) (measured only in the final timestamp instant)

    private float magneticField; //average module of the magnetic field in uT (measured during the whole interval)
    private float proximity; //distance in cm that an object is from the proximity sensor of the device (on some devices 0=near, 8=far) (measured only in the final timestamp instant)
    private float maxSpeed; //maximum speed from all the locations acquired during the interval in m/s
    private float totalDistance; //approximation of the distance travelled during the interval in meters (calculated by adding all the distances between consecutive locations (includes the last known distance before the interval started))

    private RealmList<LocationCustom> locationList; //list of locations acquired during the interval

    private RealmList<WifiDeviceCustom> wifiDevices = null; //list of devices in the same network as ours (regarding only the last scan made in the interval if there were more than 1) (lists every device with a mac address including routers)
    private int numberWifiDevices; //number of devices in the same network as ours, or -1 if no scan was done in this interval
    private RealmList<WifiNetworkCustom> wifiNetworks = null; //list of wifi networks visible (regarding only the last scan made in the interval if there were more than 1)
    private int numberWifiNetworks; //number of networks visible, or -1 if no scan was done during this interval
    private RealmList<BluetoothDeviceCustom> bluetoothDevices = null; //list of bluetooth devices visible (regarding only the last scan made in the interval if there were more than 1) (lists every discoverable bluetooth device and BLE devices as well)
    private int numberBluetoothDevices; //number of bluetooth devices visible, or -1 if no scan was done during this interval
    private String currentNetworkSSID; //SSID of the network we're currently connected to (measured only in the final timestamp instant)
    private int signalStrength; //signal strength of mobile network in dBm (measured only in the final timestamp instant)

    public SensorsEntry() {
    }

    public SensorsEntry(long beginningTimestamp, long finalTimestamp, int batteryLevel, int signalStrength, MotionValues motionValues, boolean inMotion, boolean moving, boolean display, float maxSpeed, float total_distance, String currentNetworkSSID, float magneticField, float proximity, RealmList<LocationCustom> locationList, RealmList<WifiDeviceCustom> wifiDevices, RealmList<WifiNetworkCustom> wifiNetworks, RealmList<BluetoothDeviceCustom> bluetoothDevices) {
        this.beginningTimestamp = beginningTimestamp;
        this.finalTimestamp = finalTimestamp;
        this.timestamp = new Timestamp(beginningTimestamp).toString() + " - " + new Timestamp(finalTimestamp).toString();
        this.batteryLevel = batteryLevel;

        this.inMotion = inMotion;
        this.moving = moving;
        this.display = display;

        this.currentNetworkSSID = currentNetworkSSID;

        this.proximity = proximity;

        this.signalStrength = signalStrength;

        this.maxSpeed = maxSpeed;
        this.totalDistance = total_distance;
        if(wifiNetworks != null)
            numberWifiNetworks = wifiNetworks.size();
        else
            numberWifiNetworks = -1;
        if(bluetoothDevices != null)
            numberBluetoothDevices = bluetoothDevices.size();
        else
            numberBluetoothDevices = -1;

        if(wifiDevices != null)
            numberWifiDevices = wifiDevices.size();
        else
            numberWifiDevices = -1;

        this.locationList = locationList;

        this.wifiDevices = wifiDevices;

        this.wifiNetworks = wifiNetworks;

        this.bluetoothDevices = bluetoothDevices;

        this.motionValues = motionValues;

        this.magneticField = magneticField;

        this.onServer = false;

    }

    public long getBeginningTimestamp() {
        return beginningTimestamp;
    }

    public void setBeginningTimestamp(long beginningTimestamp) {
        this.beginningTimestamp = beginningTimestamp;
    }

    public long getFinalTimestamp() {
        return finalTimestamp;
    }

    public void setFinalTimestamp(long finalTimestamp) {
        this.finalTimestamp = finalTimestamp;
    }

    public boolean isOnServer() {
        return onServer;
    }

    public void setOnServer(boolean onServer) {
        this.onServer = onServer;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public MotionValues getMotionValues() {
        return motionValues;
    }

    public void setMotionValues(MotionValues motionValues) {
        this.motionValues = motionValues;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public float getMagneticField() {
        return magneticField;
    }

    public void setMagneticField(float magneticField) {
        this.magneticField = magneticField;
    }

    public float getProximity() {
        return proximity;
    }

    public void setProximity(float proximity) {
        this.proximity = proximity;
    }

    public RealmList<WifiDeviceCustom> getWifiDevices() {
        return wifiDevices;
    }

    public void setWifiDevices(RealmList<WifiDeviceCustom> wifiDevices) {
        this.wifiDevices = wifiDevices;
    }

    public RealmList<WifiNetworkCustom> getWifiNetworks() {
        return wifiNetworks;
    }

    public void setWifiNetworks(RealmList<WifiNetworkCustom> wifiNetworks) {
        this.wifiNetworks = wifiNetworks;
    }

    public RealmList<BluetoothDeviceCustom> getBluetoothDevices() {
        return bluetoothDevices;
    }

    public void setBluetoothDevices(RealmList<BluetoothDeviceCustom> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public RealmList<LocationCustom> getLocationList() {
        return locationList;
    }

    public void setLocationList(RealmList<LocationCustom> locationList) {
        this.locationList = locationList;
    }

    public int getNumberWifiDevices() {
        return numberWifiDevices;
    }

    public void setNumberWifiDevices(int numberWifiDevices) {
        this.numberWifiDevices = numberWifiDevices;
    }

    public int getNumberWifiNetworks() {
        return numberWifiNetworks;
    }

    public void setNumberWifiNetworks(int numberWifiNetworks) {
        this.numberWifiNetworks = numberWifiNetworks;
    }

    public int getNumberBluetoothDevices() {
        return numberBluetoothDevices;
    }

    public void setNumberBluetoothDevices(int numberBluetoothDevices) {
        this.numberBluetoothDevices = numberBluetoothDevices;
    }

    public String getCurrentNetworkSSID() {
        return currentNetworkSSID;
    }

    public void setCurrentNetworkSSID(String currentNetworkSSID) {
        this.currentNetworkSSID = currentNetworkSSID;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public boolean isInMotion() {
        return inMotion;
    }

    public void setInMotion(boolean inMotion) {
        this.inMotion = inMotion;
    }





    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("SensorsEntry{" +
                "timestamp=" + timestamp +
                ", batteryLevel=" + batteryLevel +
                ", motionValues=");
        if(motionValues != null)
            string.append(motionValues.toString());
        else
            string.append("null");

        string.append(", moving=").append(moving).
                append(", signalStrength=").append(signalStrength).
                append(", display=").append(display).
                append(", magneticField=").append(magneticField).
                append(", proximity=").append(proximity).
                append(", location=");
        if(locationList != null)
        {
            for(LocationCustom l:locationList)
                string.append(l.toString());
        }
        else
            string.append("null");

        string.append(", currentNetworkSSID=");

        if(currentNetworkSSID != null)
            string.append(currentNetworkSSID);
        else
            string.append("null");


        string.append(", wifiDevices=");

        if(wifiDevices != null)
        {
            for(WifiDeviceCustom d:wifiDevices)
                string.append(d.toString());
        }
        else
            string.append("null");

        string.append(", wifiNetworks=");

        if(wifiNetworks != null)
        {
            for(WifiNetworkCustom d:wifiNetworks)
                string.append(d.toString());
        }
        else
            string.append("null");

        string.append(", bluetoothDevices=");

        if(bluetoothDevices != null)
        {
            for(BluetoothDeviceCustom d:bluetoothDevices)
                string.append(d.toString());
        }
        else
            string.append("null");

        string.append('}');

        return string.toString();


    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


}
