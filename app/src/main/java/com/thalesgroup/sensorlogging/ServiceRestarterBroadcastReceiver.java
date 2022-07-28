package com.thalesgroup.sensorlogging;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * Created by thales on 02/08/2018.
 */

/**
 * The purpose of this broadcast is to try to (re)start the service DataAcquisitionService. It does so
 * either on Boot, or on connectivity changed, or when the app is first opened or every 2min
 */
public class ServiceRestarterBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "ServiceRestarter";

    @Override
    public void onReceive(Context context, Intent intent) {


        Log.i(LOG_TAG, "Trying to (re)start service...");
        //Send an intent to this receiver in 2min to restart the service if android killed it
        Intent ownIntent = new Intent("com.thalesgroup.sensorlogging.ServiceRestarterBroadcastReceiver");
        PendingIntent pintent = PendingIntent.getBroadcast(context, 0, ownIntent, 0);
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 120*1000, pintent);
        }

        //if the service is not active, start it
        DataAcquisitionService dataAcquisitionService = new DataAcquisitionService();
        Intent mServiceIntent = new Intent(context, dataAcquisitionService.getClass());
        if (!isMyServiceRunning(context, dataAcquisitionService.getClass())) {
            Log.i(LOG_TAG, "...(re)starting service...");
            context.startService(mServiceIntent);
        }
        else
            Log.i(LOG_TAG, "...service already active.");
    }

    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {

                    return true;
                }
            }
        }

        return false;
    }
}
