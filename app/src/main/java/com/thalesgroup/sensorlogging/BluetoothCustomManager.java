package com.thalesgroup.sensorlogging;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;

/*
  Created by thales on 30/07/2018.
 */

/**
 * Manages and performs scans on bluetooth devices.
 * Does this automatically when relevant. This data can be extracted through the method
 * extractBluetoothDevicesList(). Requires the method setModeAndUpdate(int mode)
 * to be called every 10 secs or so to make the necessary updates.
 */
public class BluetoothCustomManager {

    private static final long TWO_MINUTES = 2*60*1000;
    private static final long TWENTY_MINUTES = 20*60*1000;
    private static final long FIVE_MINUTES = 5*60*1000;
    private static final long ONE_HOUR = 60*60*1000;
    private static final String LOG_TAG = "BluetoothCustomManager";
    private List<BluetoothDeviceCustom> currentBluetoothDevicesVisibleTemp = null; //temporary list in which devices are added as they are found
    private List<BluetoothDeviceCustom> currentBluetoothDevicesVisible = null; //list containing only the final list of devices at the end of a scan



    private long timeOfLastBluetoothDevicesScan = 0; //instant (in ms) in which the last scan on bluetooth devices occurred

    private final static String SHARED_PREF_TIME_BT_SCAN = "com.thalesgroup.sensorlogging.BluetoothCustomManager.timeOfLastBluetoothDevicesScan";
    private final SharedPreferences sharedPref; //database for storing internal variable(s)

    private int mode = -1; //EnergyMode
    private final Context mContext;
    private final BluetoothAdapter bluetoothAdapter;

    //Broadcast receiver for bluetooth related intents (discovery started, device found, discovery finished)
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                timeOfLastBluetoothDevicesScan = System.currentTimeMillis(); //on scan started, set the time of last scan
                //reset the lists
                currentBluetoothDevicesVisible = null;
                currentBluetoothDevicesVisibleTemp = new ArrayList<>();
                Log.i(LOG_TAG, "Bluetooth devices scan started...");
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDeviceCustom device = new BluetoothDeviceCustom((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));

                if (currentBluetoothDevicesVisibleTemp != null && !currentBluetoothDevicesVisibleTemp.contains(device))
                {
                    currentBluetoothDevicesVisibleTemp.add(device); //add device found to temporary list
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                currentBluetoothDevicesVisible = currentBluetoothDevicesVisibleTemp; //on scan finished, set definitive list to temporary list
                currentBluetoothDevicesVisibleTemp = null; //reset temporary list
                if(currentBluetoothDevicesVisible != null)
                    Log.i(LOG_TAG, "...bluetooth devices scan finished. " + currentBluetoothDevicesVisible.size() + " devices found.");
            }

        }
    };

    /**
     * Constructor
     * @param mContext - Application context
     */
    public BluetoothCustomManager(Context mContext) {

        this.mContext = mContext;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sharedPref = mContext.getSharedPreferences(DataAcquisitionService.SHARED_PREF_TAG, Context.MODE_PRIVATE);
        timeOfLastBluetoothDevicesScan = sharedPref.getLong(SHARED_PREF_TIME_BT_SCAN, 0); //get last time from shared preferences
        //regist receiver for start of scan, device found and end of scan
        mContext.registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mContext.registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        mContext.registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));


    }


    /**
     * Returns and clear the list of Bluetooth devices visible
     * @return list of Bluetooth devices visible
     */
    public RealmList<BluetoothDeviceCustom> extractBluetoothDevicesList()
    {
        List<BluetoothDeviceCustom> list = currentBluetoothDevicesVisible;
        currentBluetoothDevicesVisible = null;
        if(list == null)
            return null;
        RealmList<BluetoothDeviceCustom> realmList = new RealmList<>();
        realmList.addAll(list);
        return realmList;
    }

    /**
     * updates in the shared preferences file the values of the variables stored
     */
    public void updateSharedPreferences() {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(SHARED_PREF_TIME_BT_SCAN, timeOfLastBluetoothDevicesScan);
        editor.apply();
    }


    /**
     * Determines whether it's relevant or not to scan for bluetooth devices
     * @return true = should scan, false = shouldn't scan
     */
    private boolean shouldScanBluetoothDevices()
    {
        //if bluetooth is not enabled, don't scan
        if(!isBluetoothOn())
            return false;
        //if there's already data, don't scan
        if(currentBluetoothDevicesVisible != null)
            return false;
        //if enough time has passed that it becomes relevant to scan again, scan
        if(mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastBluetoothDevicesScan > TWO_MINUTES) //2min for High battery & In motion
            return true;
        if(mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastBluetoothDevicesScan > TWENTY_MINUTES) //20min for High battery & not In motion
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastBluetoothDevicesScan > FIVE_MINUTES) //5min for Low battery & In motion
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastBluetoothDevicesScan > ONE_HOUR) //1h for High battery & not In motion
            return true;
        return false;

    }


    /**
     * Determines whether the device has bluetooth enabled
     * @return true if enable, false if not
     */
    private boolean isBluetoothOn() {

        if (bluetoothAdapter == null) {
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update energy mode and start the scanning of bluetooth devices if necessary
     * @param mode - current EnergyMode of the device
     */
    public void setModeAndUpdate(int mode) {

        if(mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_INMOTION || mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION)
        {
            this.mode = mode;
        }

        if(shouldScanBluetoothDevices())
            scanBluetoothDevices();


    }


    /**
     * starts the scan for bluetooth devices
     */
    private void scanBluetoothDevices() {

        bluetoothAdapter.startDiscovery();
    }

    public void onDestroy()
    {
        mContext.unregisterReceiver(mBluetoothReceiver);
    }


}
