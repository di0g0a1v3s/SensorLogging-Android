package com.thalesgroup.sensorlogging;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by thales on 26/07/2018.
 */

/**
 * Manages the values of battery, display, signal strength, proximity sensor and magnetic field sensor.
 * These values can be extracted through the methods isDisplayOn(), getBatteryLevel(), getSignalStrength(),
 * getProximity() and extractMagneticField().
 */
public class VariousSensorsCustomManager implements SensorEventListener {


    private static final int SENSORS_DELAY = 2000000; //2 sec (us)

    private int mSignalStrength = 0;
    private final TelephonyManager mTelephonyManager;
    private final static String SHARED_PREF_SIGNAL_STRENGTH = "com.thalesgroup.sensorlogging.VariousSensorsCustomManager.mSignalStrength";
    private float currentProximityFromObject = 0;
    private final static String SHARED_PREF_PROXIMITY = "com.thalesgroup.sensorlogging.VariousSensorsCustomManager.currentProximityFromObject";

    private List<Float> magneticFieldList = new ArrayList<>();
    private final SensorManager mSensorManager;
    private final Context mContext; //Application context
    private final SharedPreferences sharedPref;

    /**
     * Constructor
     * @param mContext - Application Context
     */
    public VariousSensorsCustomManager(Context mContext) {
        this.mContext = mContext;
        this.mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        enableSensors();
        MyPhoneStateListener mPhoneStatelistener = new MyPhoneStateListener();
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        if (mTelephonyManager != null && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED))
        {
            mTelephonyManager.listen(mPhoneStatelistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
        sharedPref = mContext.getSharedPreferences(DataAcquisitionService.SHARED_PREF_TAG, Context.MODE_PRIVATE);
        mSignalStrength = sharedPref.getInt(SHARED_PREF_SIGNAL_STRENGTH, 0);
        currentProximityFromObject = sharedPref.getFloat(SHARED_PREF_PROXIMITY, 0);


    }

    /**
     * updates in the shared preferences file the values of the variables stored
     */
    public void updateSharedPreferences() {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SHARED_PREF_SIGNAL_STRENGTH, mSignalStrength);
        editor.putFloat(SHARED_PREF_PROXIMITY, currentProximityFromObject);
        editor.apply();
    }

    /**
     * enables the magnetic field and proximity sensors
     */
    private void enableSensors()
    {
        if(getSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
            mSensorManager.registerListener(this, getSensor(Sensor.TYPE_MAGNETIC_FIELD), SENSORS_DELAY);
        if(getSensor(Sensor.TYPE_PROXIMITY) != null)
            mSensorManager.registerListener(this, getSensor(Sensor.TYPE_PROXIMITY), SENSORS_DELAY);
    }

    /**
     * @param sensor - Sensor type
     * @return default sensor from type required or null
     */
    @Nullable
    private Sensor getSensor(int sensor)
    {
        if (mSensorManager != null && mSensorManager.getDefaultSensor(sensor) != null){
            //sensor exists
            return mSensorManager.getDefaultSensor(sensor);
        }
        else {
            return null;
        }

    }

    /**
     * determines whether display is on or off
     * @return true if on, false if off
     */
    public boolean isDisplayOn()
    {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            return pm.isScreenOn();
        }
        return false;

    }

    /**
     * determines the battery level of the device and whether it is charging
     * @return positive integer (0:100) if charging, negative integer (-100:-0) if not charging
     */
    public int getBatteryLevel()
    {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);
        // Are we charging / charged?
        int status = 0;
        int level = 0;
        int scale = 0;
        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;


        int batteryPct = (int)(level * 100 / (float)scale);

        if(isCharging)
            return batteryPct;
        else
        {
            return -batteryPct;

        }

    }

    /**
     * @return proximity from an object in cm measured by the proximity sensor
     */
    public float getProximity()
    {
        return currentProximityFromObject;
    }


    /**
     * @return current signal strength of mobile network
     */
    public int getSignalStrength()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<CellInfo> cellInfoList = mTelephonyManager.getAllCellInfo();
            for(CellInfo c:cellInfoList)
            {
                if(c.isRegistered())
                {
                    if(c.getClass().equals(CellInfoLte.class))
                    {
                        CellInfoLte cellInfo = (CellInfoLte) c;
                        mSignalStrength = cellInfo.getCellSignalStrength().getDbm();
                    }
                    else if(c.getClass().equals(CellInfoGsm.class))
                    {
                        CellInfoGsm cellInfo = (CellInfoGsm) c;
                        mSignalStrength = cellInfo.getCellSignalStrength().getDbm();
                    }
                    else if(c.getClass().equals(CellInfoCdma.class))
                    {
                        CellInfoCdma cellInfo = (CellInfoCdma) c;
                        mSignalStrength = cellInfo.getCellSignalStrength().getDbm();
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && c.getClass().equals(CellInfoWcdma.class)) {

                        CellInfoWcdma cellInfo = (CellInfoWcdma) c;
                        mSignalStrength = cellInfo.getCellSignalStrength().getDbm();

                    }
                    return mSignalStrength;


                }
            }
        }

        return mSignalStrength;
    }

    /**
     * Calculates the average magnetic field since the last extract and clears the list
     * @return average magnetic field intensity
     */
    public float extractMagneticField()
    {
        List<Float> magneticFieldListTemp = magneticFieldList;
        magneticFieldList = new ArrayList<>();
        return MathExtra.listAvg(magneticFieldListTemp);

    }

    public void onDestroy()
    {
        disableSensors();
    }

    private void disableSensors() {
        mSensorManager.unregisterListener(this);
    }

    //---------------------SensorEventListener--------------------------
    /**
     * updates the values of currentProximityFromObject and magneticFieldList according to the type of sensor fired
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY)
        {
            currentProximityFromObject = sensorEvent.values[0];
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            float[] currentMagneticFieldVector = new float[3];
            System.arraycopy(sensorEvent.values, 0, currentMagneticFieldVector, 0, 3);

            magneticFieldList.add(MathExtra.vectorModule(currentMagneticFieldVector));
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //Only used in versions of android earlier than JELLY_BEAN_MR1 (not sure if it works)
    class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED))
            {
                mSignalStrength = signalStrength.getGsmSignalStrength();
                mSignalStrength = (2 * mSignalStrength) - 113; // -> dBm
            }



        }
    }
}
