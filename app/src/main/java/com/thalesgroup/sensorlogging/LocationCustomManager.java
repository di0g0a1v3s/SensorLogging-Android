package com.thalesgroup.sensorlogging;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.Nullable;

import io.realm.RealmList;

/*
  Created by thales on 25/07/2018.
 */

/**
 * Manages and performs scans on location from the various providers.
 * Does this automatically when relevant. This data can be extracted through the method
 * extractLocationList() and extractTotalDistance(). Requires the method setModeAndUpdate(int mode)
 * to be called every 10 secs or so to make the necessary updates.
 */
public class LocationCustomManager implements LocationListener {

    private static final int CYCLE_DURATION = DataAcquisitionService.DELAY_DB; //duration of a cycle (1min) (milliseconds)
    private static final int NUMBER_CYCLES_SAVED = 3; //number of cycles saved in currentLocationList

    private static final long NUMBER_LOCATIONS_THRESHOLD = 5; //number of locations from a single provider during 1 cycle, when device is moving, that make other providers obsolete
    private static final float EARTH_RADIUS = 6371000; // meters
    private static final float ACCURACY_THRESHOLD = 40; //max accuracy for a location to be accounted for by this manager  (meters)


    private static final int MIN_TIME_BTW_LOCATIONS_FOR_SIGNIFICANT = 1000 * 60 * 2; //minimum time between two location updates for one to be significantly newer than the other (2min) (milliseconds)


    private static final int MIN_DISTANCE_LOCATION = 0; //distance between locations for update (meters)
    private static final String LOG_TAG = "LocationCustomManager";
    private static final float SPEED_FOR_MOVING_THRESHOLD = 1.2f; //minimum speed for motion (m/s) ~walking speed

    private float totalDistance = 0.0f; //total distance travelled since last extract

    private int min_time_location; //time between locations for updates
    private String primaryLocationProvider = "";
    private final Context mContext;
    private int mode = -1; //EnergyMode


    private long timeGpsAuxiliaryProviderOn = 0;
    private final static String SHARED_PREF_TIME_GPS_ON = "com.thalesgroup.sensorlogging.LocationCustomManager.timeGpsAuxiliaryProviderOn";
    private long timeGpsAuxiliaryProviderOff = 0;
    private final static String SHARED_PREF_TIME_GPS_OFF = "com.thalesgroup.sensorlogging.LocationCustomManager.timeGpsAuxiliaryProviderOff";
    private boolean gpsAuxiliaryProviderEnabled = false;
    private final static String SHARED_PREF_GPS_AP = "com.thalesgroup.sensorlogging.LocationCustomManager.gpsAuxiliaryProviderEnabled";
    private long timeNetworkAuxiliaryProviderOn = 0;
    private final static String SHARED_PREF_TIME_NETWORK_ON = "com.thalesgroup.sensorlogging.LocationCustomManager.timeNetworkAuxiliaryProviderOn";
    private long timeNetworkAuxiliaryProviderOff = 0;
    private final static String SHARED_PREF_TIME_NETWORK_OFF = "com.thalesgroup.sensorlogging.LocationCustomManager.timeNetworkAuxiliaryProviderOff";
    private boolean networkAuxiliaryProviderEnabled = false;
    private final static String SHARED_PREF_NETWORK_AP = "com.thalesgroup.sensorlogging.LocationCustomManager.networkAuxiliaryProviderEnabled";
    private long timeIdleStart = 0;
    private final static String SHARED_PREF_TIME_IDLE_ON = "com.thalesgroup.sensorlogging.LocationCustomManager.timeIdleStart";
    private long timeIdleStop = 0;
    private final static String SHARED_PREF_TIME_IDLE_OFF = "com.thalesgroup.sensorlogging.LocationCustomManager.timeIdleStop";
    private long timeNotInMotionStarted = 0;
    private final static String SHARED_PREF_TIME_NOT_MOTION_START = "com.thalesgroup.sensorlogging.LocationCustomManager.timeNotInMotionStarted";
    private boolean idle = false;
    private final static String SHARED_PREF_IDLE = "com.thalesgroup.sensorlogging.LocationCustomManager.idle";
    private long timeOfLastLocationUpdate = 0;
    private final static String SHARED_PREF_TIME_LAST_LOCATION_UPDATE = "com.thalesgroup.sensorlogging.LocationCustomManager.timeOfLastLocationUpdate";
    private LocationCustom lastLocation = null;
    private final static String SHARED_PREF_LAST_LOCATION_LATITUDE = "com.thalesgroup.sensorlogging.LocationCustomManager.lastLocation.latitude";
    private final static String SHARED_PREF_LAST_LOCATION_LONGITUDE = "com.thalesgroup.sensorlogging.LocationCustomManager.lastLocation.longitude";
    private final static String SHARED_PREF_LAST_LOCATION_SPEED = "com.thalesgroup.sensorlogging.LocationCustomManager.lastLocation.lastLocation.speed";
    private final static String SHARED_PREF_LAST_LOCATION_ACCURACY = "com.thalesgroup.sensorlogging.LocationCustomManager.lastLocation.lastLocation.accuracy";

    private boolean moving = false; //indicates whether device is "travelling"

    private final SharedPreferences sharedPref;

    private List<LocationCustom> currentLocationList = null; //list of locations since NUMBER_CYCLES_SAVED cycles ago
    private List<LocationCustom> currentLocationListToReturn = null; //list of locations since last extract

    private final LocationManager mLocationManager;
    private final WifiCustomManager mWifiCustomManager;



    /**
     * Constructor
     * @param mContext - Application Context
     * @param mWifiCustomManager - WifiCustomManager object
     */
    public LocationCustomManager(Context mContext, WifiCustomManager mWifiCustomManager) {
        this.mContext = mContext;
        this.mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        this.mWifiCustomManager = mWifiCustomManager;
        sharedPref = mContext.getSharedPreferences(DataAcquisitionService.SHARED_PREF_TAG, Context.MODE_PRIVATE);
        //extract saved data from shared preferences
        timeGpsAuxiliaryProviderOn = sharedPref.getLong(SHARED_PREF_TIME_GPS_ON, 0);
        timeGpsAuxiliaryProviderOff = sharedPref.getLong(SHARED_PREF_TIME_GPS_OFF, 0);
        gpsAuxiliaryProviderEnabled = sharedPref.getBoolean(SHARED_PREF_GPS_AP, false);
        timeNetworkAuxiliaryProviderOn = sharedPref.getLong(SHARED_PREF_TIME_NETWORK_ON, 0);
        timeNetworkAuxiliaryProviderOff = sharedPref.getLong(SHARED_PREF_TIME_NETWORK_OFF, 0);
        networkAuxiliaryProviderEnabled = sharedPref.getBoolean(SHARED_PREF_NETWORK_AP, false);
        timeIdleStart = sharedPref.getLong(SHARED_PREF_TIME_IDLE_ON, 0);
        timeIdleStop = sharedPref.getLong(SHARED_PREF_TIME_IDLE_OFF, System.currentTimeMillis());
        idle = sharedPref.getBoolean(SHARED_PREF_IDLE, false);
        timeOfLastLocationUpdate = sharedPref.getLong(SHARED_PREF_TIME_LAST_LOCATION_UPDATE, 0);
        timeNotInMotionStarted = sharedPref.getLong(SHARED_PREF_TIME_NOT_MOTION_START, 0);

        float lastLocationLatitude = sharedPref.getFloat(SHARED_PREF_LAST_LOCATION_LATITUDE, Float.NaN);
        float lastLocationLongitude = sharedPref.getFloat(SHARED_PREF_LAST_LOCATION_LONGITUDE, Float.NaN);
        float lastLocationSpeed = sharedPref.getFloat(SHARED_PREF_LAST_LOCATION_SPEED, Float.NaN);
        float lastLocationAccuracy = sharedPref.getFloat(SHARED_PREF_LAST_LOCATION_ACCURACY, Float.NaN);
        if(!Float.isNaN(lastLocationLatitude) && !Float.isNaN(lastLocationLongitude) && !Float.isNaN(lastLocationSpeed) && !Float.isNaN(lastLocationAccuracy))
        {
            lastLocation = new LocationCustom();
            lastLocation.setAccuracy(lastLocationAccuracy);
            lastLocation.setSpeed(lastLocationSpeed);
            lastLocation.setLatitude(lastLocationLatitude);
            lastLocation.setLongitude(lastLocationLongitude);
            lastLocation.setTimestamp(timeOfLastLocationUpdate);
        }
    }

    /**
     * Update energy mode and location providers enabled/disabled and their delays
     * @param mode - current EnergyMode of the device
     */
    public void setModeAndUpdate(int mode) {

        boolean update = false;
        if(this.mode != mode && (mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_INMOTION || mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION))
        {
            primaryLocationProvider = "";
            switch (mode)
            {
                case EnergyModes.MODE_HIGH_BATTERY_INMOTION:
                    min_time_location = 5000;
                    break;
                case EnergyModes.MODE_LOW_BATTERY_INMOTION:
                    min_time_location = 10000;
                    break;
                case EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION:
                    timeNotInMotionStarted = System.currentTimeMillis();
                    min_time_location = 5000;
                    break;
                case EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION:
                    timeNotInMotionStarted = System.currentTimeMillis();
                    min_time_location = 10000;
                    break;

            }

            this.mode = mode;
            update = true;
        }

        deleteOldLocations(); //update location list
        if(moving != isMoving(totalDistance, currentLocationList, lastLocation))
            moving = !moving;


        if(changeInIdle() | changeInAuxiliaryProviders() | update)
            updateLocationProviders();

        //scanning wifi networks helps getting a location from network provider
        if(networkAuxiliaryProviderEnabled)
            mWifiCustomManager.scanWifiNetworks();

    }

    /**
     * updates in the shared preferences file the values of the variables stored
     */
    public void updateSharedPreferences() {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(SHARED_PREF_TIME_GPS_ON, timeGpsAuxiliaryProviderOn);
        editor.putLong(SHARED_PREF_TIME_GPS_OFF, timeGpsAuxiliaryProviderOff);
        editor.putLong(SHARED_PREF_TIME_NETWORK_ON, timeNetworkAuxiliaryProviderOn);
        editor.putLong(SHARED_PREF_TIME_NETWORK_OFF, timeNetworkAuxiliaryProviderOff);
        editor.putLong(SHARED_PREF_TIME_IDLE_ON, timeIdleStart);
        editor.putLong(SHARED_PREF_TIME_IDLE_OFF, timeIdleStop);
        editor.putLong(SHARED_PREF_TIME_LAST_LOCATION_UPDATE, timeOfLastLocationUpdate);
        editor.putLong(SHARED_PREF_TIME_NOT_MOTION_START, timeNotInMotionStarted);
        editor.putBoolean(SHARED_PREF_GPS_AP, gpsAuxiliaryProviderEnabled);
        editor.putBoolean(SHARED_PREF_NETWORK_AP, networkAuxiliaryProviderEnabled);
        editor.putBoolean(SHARED_PREF_IDLE, idle);
        if(lastLocation != null) {
            editor.putFloat(SHARED_PREF_LAST_LOCATION_SPEED, lastLocation.getSpeed());
            editor.putFloat(SHARED_PREF_LAST_LOCATION_ACCURACY, lastLocation.getAccuracy());
            editor.putFloat(SHARED_PREF_LAST_LOCATION_LONGITUDE, (float)lastLocation.getLongitude());
            editor.putFloat(SHARED_PREF_LAST_LOCATION_LATITUDE, (float)lastLocation.getLatitude());
        }
        editor.apply();
    }


    /**
     * enables/disables location providers based on networkAuxiliaryProviderEnabled,
     * gpsAuxiliaryProviderEnabled and idle and updates their delay
     */
    private void updateLocationProviders()
    {
        //when idle, all updates on location are dismissed
        if(idle)
        {
            mLocationManager.removeUpdates(this);
            return;
        }

        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mLocationManager.removeUpdates(this);
            if(mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
            {
                mLocationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER, min_time_location, MIN_DISTANCE_LOCATION, this);
                primaryLocationProvider = LocationManager.PASSIVE_PROVIDER;
            }
            else if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, min_time_location, MIN_DISTANCE_LOCATION, this);
                primaryLocationProvider = LocationManager.NETWORK_PROVIDER;
            }
            else if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, min_time_location, MIN_DISTANCE_LOCATION, this);
                primaryLocationProvider = LocationManager.GPS_PROVIDER;
            }

            //auxiliary providers
            if(networkAuxiliaryProviderEnabled && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 0, MIN_DISTANCE_LOCATION, this);
            }

            if(gpsAuxiliaryProviderEnabled && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, MIN_DISTANCE_LOCATION, this);
            }


        }
        Log.i(LOG_TAG, "Primary location provider: " + primaryLocationProvider);
    }


    /**
     * @return true if a change in the auxiliary providers is necessary
     */
    private boolean changeInAuxiliaryProviders()
    {
        boolean update = false;
        //idle
        if(idle)
        {
            if(gpsAuxiliaryProviderEnabled)
            {
                gpsAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "GPS auxiliary provider is now off");
                timeGpsAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
            if(networkAuxiliaryProviderEnabled)
            {
                networkAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "Network auxiliary provider is now off");
                timeNetworkAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
        }
        //High battery
        if((mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION) && !idle)
        {
            //disable network auxiliary provider if it's been on for more than half a cycle
            if(networkAuxiliaryProviderEnabled && System.currentTimeMillis() - timeNetworkAuxiliaryProviderOn > CYCLE_DURATION/2)
            {
                networkAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "Network auxiliary provider is now off");
                timeNetworkAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
            //enable network auxiliary provider if it's been off for more than half a cycle and the last update on location was over half a cycle ago
            if(!networkAuxiliaryProviderEnabled && (System.currentTimeMillis() - timeNetworkAuxiliaryProviderOff > CYCLE_DURATION/2) &&
                    (System.currentTimeMillis() - timeOfLastLocationUpdate > CYCLE_DURATION/2))
            {
                networkAuxiliaryProviderEnabled = true;
                Log.i(LOG_TAG, "Network auxiliary provider is now on");
                timeNetworkAuxiliaryProviderOn = System.currentTimeMillis();
                update = true;
            }
            //disable gps auxiliary provider if it's been on for more than a quarter cycle
            if(gpsAuxiliaryProviderEnabled && System.currentTimeMillis() - timeGpsAuxiliaryProviderOn > CYCLE_DURATION/4)
            {
                gpsAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "GPS auxiliary provider is now off");
                timeGpsAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
            //enable gps auxiliary provider if it's been off for more than 3/4 cycles and the last update on location was over 3/4 cycles ago
            if(!gpsAuxiliaryProviderEnabled && (System.currentTimeMillis() - timeGpsAuxiliaryProviderOff > 3*CYCLE_DURATION/4) &&
                    (System.currentTimeMillis() - timeOfLastLocationUpdate > 3*CYCLE_DURATION/4))
            {
                gpsAuxiliaryProviderEnabled = true;
                Log.i(LOG_TAG, "GPS auxiliary provider is now on");
                timeGpsAuxiliaryProviderOn = System.currentTimeMillis();
                update = true;
            }
        }

        //Low Battery
        if((mode == EnergyModes.MODE_LOW_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION) && !idle)
        {
            //disable network auxiliary provider if it's been on for more than half a cycle
            if(networkAuxiliaryProviderEnabled && System.currentTimeMillis() - timeNetworkAuxiliaryProviderOn > CYCLE_DURATION/2)
            {
                networkAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "Network auxiliary provider is now off");
                timeNetworkAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
            //enable network auxiliary provider if it's been off for more than (1 + 1/2) cycles and the last update on location was over (1 + 1/2) cycles ago
            if(!networkAuxiliaryProviderEnabled && (System.currentTimeMillis() - timeNetworkAuxiliaryProviderOff > 3*CYCLE_DURATION/2) &&
                    (System.currentTimeMillis() - timeOfLastLocationUpdate > 3*CYCLE_DURATION/2))
            {
                networkAuxiliaryProviderEnabled = true;
                Log.i(LOG_TAG, "Network auxiliary provider is now on");
                timeNetworkAuxiliaryProviderOn = System.currentTimeMillis();
                update = true;
            }
            //disable gps auxiliary provider if it's been on for more than a quarter cycle
            if(gpsAuxiliaryProviderEnabled && System.currentTimeMillis() - timeGpsAuxiliaryProviderOn > CYCLE_DURATION/4)
            {
                gpsAuxiliaryProviderEnabled = false;
                Log.i(LOG_TAG, "GPS auxiliary provider is now off");
                timeGpsAuxiliaryProviderOff = System.currentTimeMillis();
                update = true;
            }
            //enable gps auxiliary provider if it's been off for more than (1+3/4)cycles and the last update on location was over (1+3/4)cycles ago
            if(!gpsAuxiliaryProviderEnabled && (System.currentTimeMillis() - timeGpsAuxiliaryProviderOff > 7*CYCLE_DURATION/4) &&
                    (System.currentTimeMillis() - timeOfLastLocationUpdate > 7*CYCLE_DURATION/4))
            {
                gpsAuxiliaryProviderEnabled = true;
                Log.i(LOG_TAG, "GPS auxiliary provider is now on");
                timeGpsAuxiliaryProviderOn = System.currentTimeMillis();
                update = true;
            }
        }
        //moving
        if(moving)
        {
            //calculate gps and network "points" : number of locations received from each provider in the last cycle
            int gpsPoints = 0;
            int networkPoints = 0;
            if(currentLocationList != null)
            {
                for(LocationCustom location:currentLocationList) {
                    if (location.getTimestamp() > System.currentTimeMillis() - CYCLE_DURATION) {
                        if (location.getProvider().equals("gps"))
                            gpsPoints++;
                        else if (location.getProvider().equals("network"))
                            networkPoints++;
                    }
                }
            }
            boolean enableNetwork = false;
            boolean enableGPS = false;

            if(networkPoints > NUMBER_LOCATIONS_THRESHOLD) //priority: network provider
                enableNetwork = true;
            else if(gpsPoints > NUMBER_LOCATIONS_THRESHOLD)
                enableGPS = true;
            else //not enough points from either : enable both
            {
                enableNetwork = true;
                enableGPS = true;
            }
            if(!networkAuxiliaryProviderEnabled && enableNetwork)
            {
                networkAuxiliaryProviderEnabled = true;
                timeNetworkAuxiliaryProviderOn = System.currentTimeMillis();
                Log.i(LOG_TAG, "Network auxiliary provider is now on");
                update = true;
            }
            if(!gpsAuxiliaryProviderEnabled && enableGPS)
            {
                gpsAuxiliaryProviderEnabled = true;
                timeGpsAuxiliaryProviderOn = System.currentTimeMillis();
                Log.i(LOG_TAG, "GPS auxiliary provider is now on");
                update = true;
            }
        }
        return update;
    }


    /**
     * @return true if a change in the idle state is necessary
     */
    private boolean changeInIdle()
    {
        //exit idle state if moving
        if(moving && idle)
        {
            idle = false;
            timeIdleStop = System.currentTimeMillis();
            Log.i(LOG_TAG, "Idle state has ended");
            return true;
        }
        boolean update = false;
        //exit idle state if in motion
        if((mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_INMOTION) && idle)
        {
            idle = false;
            timeIdleStop = System.currentTimeMillis();
            update = true;
            Log.i(LOG_TAG, "Idle state has ended");
        }
        //exit idle state if not in motion but has been idle for over 55 cycles
        if((mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION) && idle && System.currentTimeMillis() - timeIdleStart > 55*CYCLE_DURATION)
        {
            idle = false;
            timeIdleStop = System.currentTimeMillis();
            update = true;
            Log.i(LOG_TAG, "Idle state has ended");
        }
        //enter idle state if not in motion and not idle for 5 cycles
        if((mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION) && !idle && System.currentTimeMillis() - timeIdleStop > 5 * CYCLE_DURATION && System.currentTimeMillis() - timeNotInMotionStarted > 5 * CYCLE_DURATION)
        {
            idle = true;
            timeIdleStart = System.currentTimeMillis();
            update = true;
            Log.i(LOG_TAG, "Idle state has started");
        }
        return update;

    }



    /**
     * @param lat1 - latitude of the first coordinate (angles)
     * @param long1 - longitude of the first coordinate (angles)
     * @param lat2 - latitude of the second coordinate (angles)
     * @param long2 - longitude of the second coordinate (angles)
     * @return the approximate distance in meters between two coordinates in angles
     */
    private float distanceBetweenCoordinates(double lat1, double long1, double lat2, double long2)
    {
        float[] results = new float[1];
        Location.distanceBetween(lat1, long1, lat2, long2, results);
        return results[0];
        /*double phi_1 = Math.toRadians(lat1);
        double phi_2 = Math.toRadians(lat2);
        double lambda_2 = Math.toRadians(long2);
        double lambda_1 = Math.toRadians(long1);
        double x = (lambda_2-lambda_1) * Math.cos((phi_1+phi_2)/2);
        double y = (phi_1 - phi_2);
        return Math.sqrt(x*x + y*y) * EARTH_RADIUS;*/
    }

    /**
     * @return the best location from the last known locations of the various providers
     */
    private Location determineLastKnownLocation()
    {
        Location lastKnownLocation = null;
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            if(isBetterLocation(mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER), mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)))
                lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            else
                lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(isBetterLocation(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), lastKnownLocation))
                lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        }
        return lastKnownLocation;
    }


    /**
     * Determines whether one Location reading is better than other
     * @param location1 - first location
     * @param location2 - second location
     * @return true if first location is better than second, false if second is better than first
     */
    private boolean isBetterLocation(Location location1, Location location2) {
        if(location1 == null)
        {
            return false;
        }
        if (location2 == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location1.getTime() - location2.getTime();
        boolean isSignificantlyNewer = timeDelta > MIN_TIME_BTW_LOCATIONS_FOR_SIGNIFICANT;
        boolean isSignificantlyOlder = timeDelta < -MIN_TIME_BTW_LOCATIONS_FOR_SIGNIFICANT;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location1.getAccuracy() - location2.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location1.getProvider(),
                location2.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }


    /**
     * @param provider1 first provider
     * @param provider2 second provider
     * @return true if providers are the same, false if not
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * Returns and clears the best location found since the last extract
     * @return best location
     */
    @Nullable
    public RealmList<LocationCustom> extractLocationList()
    {
        //deleteOldLocations();
        if(currentLocationListToReturn == null)
            return  null;
        //list with locations acquired during last cycle
        RealmList<LocationCustom> realmList = new RealmList<>();
        List<LocationCustom> currentLocationListTemp = new ArrayList<>(currentLocationListToReturn);
        currentLocationListToReturn = null;
        for (LocationCustom location : currentLocationListTemp) {
            if (location.getTimestamp() >= System.currentTimeMillis() - CYCLE_DURATION && location.getTimestamp() <= System.currentTimeMillis())
                realmList.add(location);
        }
        return realmList;
    }



    public void onDestroy()
    {
        disableLocationUpdates();
    }

    private void disableLocationUpdates() {
        mLocationManager.removeUpdates(this);
    }


    /*
     * determines whether device is moving
     * @return true if it is, false if it isn't
     */

    /**
     * determines whether device is moving
     * @param totalDistance
     * @param locationsList
     * @param lastLocation
     * @return
     */
    public boolean isMoving(float totalDistance, List<LocationCustom> locationsList, LocationCustom lastLocation)
    {

        //is moving if the total distance is bigger than the distance travelled in one cycle at constant speed = SPEED_FOR_MOVING_THRESHOLD
        if(totalDistance > (CYCLE_DURATION/1000 * SPEED_FOR_MOVING_THRESHOLD))
            return true;

        //is moving if the last location occurred in the last cycle and its speed if bigger than SPEED_FOR_MOVING_THRESHOLD
        if(lastLocation != null && lastLocation.getTimestamp() > System.currentTimeMillis() - CYCLE_DURATION * NUMBER_CYCLES_SAVED && lastLocation.getSpeed() > SPEED_FOR_MOVING_THRESHOLD)
            return true;

        if(locationsList != null)
        {
            LocationCustom locationPrevious = null;
            List<LocationCustom> currentLocationListTemp = new ArrayList<>(locationsList);
            for (LocationCustom location : currentLocationListTemp) {
                //is moving if speed is bigger than SPEED_FOR_MOVING_THRESHOLD ~ walking speed
                if (location.getSpeed() > SPEED_FOR_MOVING_THRESHOLD)
                    return true;

                //if the distance between consecutive locations is bigger than their accuracy, we assume the person is moving
                if (locationPrevious != null && distanceBetweenCoordinates(location.getLatitude(), location.getLongitude(),
                        locationPrevious.getLatitude(), locationPrevious.getLongitude()) > Math.max(location.getAccuracy(), locationPrevious.getAccuracy()))
                    return true;

                locationPrevious = location;
            }
        }

        return false;
    }

    /**
     * delete from currentLocationList location older than CYCLE_DURATION * NUMBER_CYCLES_SAVED
     */
    private void deleteOldLocations() {
        if(currentLocationList != null){
            ListIterator<LocationCustom> iter = currentLocationList.listIterator();
            while(iter.hasNext()){
                if(iter.next().getTimestamp() < System.currentTimeMillis() - CYCLE_DURATION * NUMBER_CYCLES_SAVED){
                    iter.remove();
                }
                else
                    break;
            }
        }
    }

    /**
     * returns and resets total distance travelled since last extract
     * @return total distance
     */
    public float extractTotalDistance()
    {
        float totalDistanceTemp = totalDistance;
        totalDistance = 0.0f;
        return  totalDistanceTemp;
    }


    public boolean isIdle() {
        return idle;
    }



    //-----------------LocationListener------------------------------------------------
    /**
     * disables the auxiliary providers and updates the currentBestLocation (if new location is better)
     * @param location new location found
     */
    @Override
    public void onLocationChanged(Location location) {

        //locations with very bad accuracy are ignored
        if(location.getAccuracy() <= ACCURACY_THRESHOLD) {
            timeOfLastLocationUpdate = location.getTime();

            //update totalDistance
            if(lastLocation != null)
                totalDistance = (totalDistance + distanceBetweenCoordinates(lastLocation.getLatitude(), lastLocation.getLongitude(), location.getLatitude(), location.getLongitude()));

            lastLocation = new LocationCustom(location);
            Log.i(LOG_TAG, "Location found from provider " + location.getProvider() + ".");

            //remove auxiliary providers (except if moving)
            if ((gpsAuxiliaryProviderEnabled || networkAuxiliaryProviderEnabled) && !moving) {
                if (gpsAuxiliaryProviderEnabled) {
                    Log.i(LOG_TAG, "GPS auxiliary provider is now off");
                    gpsAuxiliaryProviderEnabled = false;
                    timeGpsAuxiliaryProviderOff = System.currentTimeMillis();
                }
                if (networkAuxiliaryProviderEnabled) {
                    Log.i(LOG_TAG, "Network auxiliary provider is now off");
                    networkAuxiliaryProviderEnabled = false;
                    timeNetworkAuxiliaryProviderOff = System.currentTimeMillis();
                }
                updateLocationProviders();
            }

            LocationCustom locationCustom = new LocationCustom(location);

            if (currentLocationList == null)
                currentLocationList = new ArrayList<>();

            if(!currentLocationList.contains(locationCustom))
                currentLocationList.add(locationCustom);

            if (currentLocationListToReturn == null)
                currentLocationListToReturn = new ArrayList<>();

            if(!currentLocationListToReturn.contains(locationCustom))
                currentLocationListToReturn.add(locationCustom);

        }

    }


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    //on providers enabled and disabled, location providers are updated
    @Override
    public void onProviderEnabled(String s) {
        updateLocationProviders();
    }

    @Override
    public void onProviderDisabled(String s) {
        updateLocationProviders();
    }


}
