package com.thalesgroup.sensorlogging;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmAsyncTask;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;


/**
 * Service that runs in the background and handles everything: updates sensor managers, extracts data from managers, fills the database and sends information to server
 */
public class DataAcquisitionService extends Service {

    public static final int DELAY_DB = 60*1000; //interval of time between each new entry in database (1 minute) (milliseconds)
    private static final String LOG_TAG = "DataAcquisitionService";
    public static final String SHARED_PREF_TAG = "com.thalesgroup.sensorlogging.DataAcquisitionService"; //Tag for shared preferences
    private static final String SERVER_URL = null; //Server url
    private static final int DELAY_UPDATER = 10*1000; //interval of time between updates for managers (10s) (milliseconds)
    private static final int DELAY_SERVER = 60*60*1000; //interval of time between updates for server (1h) (milliseconds)

    //handlers and runnables
    private Handler databaseHandler;
    private Runnable databaseRunnable;
    private Handler serverHandler;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private Runnable serverRunnable;

    //Realm (database)
    private Realm realm;
    private RealmAsyncTask transaction;

    //sensor managers
    private MotionCustomManager mMotionCustomManager;
    private LocationCustomManager mLocationCustomManager;
    private VariousSensorsCustomManager mVariousSensorsCustomManager;
    private WifiCustomManager mWifiCustomManager;
    private BluetoothCustomManager mBluetoothCustomManager;


    private long beginningTime = 0; //instant of beginning of new entry (ms)

    /**
     * empty constructor
     */
    public DataAcquisitionService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        //initialize managers
        mMotionCustomManager = new MotionCustomManager(getApplicationContext());
        mVariousSensorsCustomManager = new VariousSensorsCustomManager(getApplicationContext());
        mWifiCustomManager = new WifiCustomManager(getApplicationContext());
        mBluetoothCustomManager = new BluetoothCustomManager(getApplicationContext());
        mLocationCustomManager = new LocationCustomManager(getApplicationContext(), mWifiCustomManager);

        updateManagers();

        // Initialize Realm
        Realm.init(getApplicationContext());

        RealmConfiguration config = Realm.getDefaultConfiguration();

        // Get a Realm instance for this thread
        if (config != null) {
            RealmAsyncTask task = Realm.getInstanceAsync(config, new Realm.Callback() {
                @Override
                public void onSuccess(Realm r) {
                    // Realm is opened and ready on the caller thread.
                    realm = r;
                }
            });
        }

        //set up update handler to be called every 10s
        updateHandler = new Handler();
        updateRunnable = new Runnable(){
            public void run(){
                updateManagers();
                updateHandler.postDelayed(this, DELAY_UPDATER);
            }
        };
        updateHandler.postDelayed(updateRunnable, DELAY_UPDATER);

        //set up database handler to be called every 1min
        databaseHandler = new Handler();
        databaseRunnable = new Runnable(){
            public void run(){
                updateDatabase();
            }
        };
        databaseHandler.postDelayed(databaseRunnable, DELAY_DB);

        //set up server handler to be called every 1hr
        serverHandler = new Handler();
        serverRunnable = new Runnable() {
            @Override
            public void run() {
                if(mWifiCustomManager.isWifiConnected() && isConnected()) //send only if user is connected through wifi
                {
                    sendToServer();
                }

                serverHandler.postDelayed(this, DELAY_SERVER);
            }
        };
        serverHandler.postDelayed(serverRunnable, DELAY_SERVER);

        beginningTime = System.currentTimeMillis();//set beginning time

        Log.i(LOG_TAG, "...service started");

        return START_STICKY;
    }

    /**
     * asynchronously send to server every entry in the database and delete the entries sent
     */
    private void sendToServer() {

        if(realm != null)
        {
            transaction = realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realmAsync) {
                    RealmResults<SensorsEntry> results = realmAsync.where(SensorsEntry.class).equalTo("onServer", false).findAll(); //all entries in database that are not in server
                    for(SensorsEntry entry:results)
                    {
                        boolean result = POST(SERVER_URL, entry); //send to server
                        if(result)
                            entry.setOnServer(true); //if sent was successful, set onServer true
                    }
                    //delete all entries in which onServer == true
                    RealmResults<SensorsEntry> entriesToDelete = realmAsync.where(SensorsEntry.class).equalTo("onServer", true).findAll();
                    deleteFromDatabase(entriesToDelete, realmAsync);
                }
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //intent to restart the service
        Intent broadcastIntent = new Intent("com.thalesgroup.sensorlogging.ServiceRestarterBroadcastReceiver");
        sendBroadcast(broadcastIntent);
        //remove callbacks from handlers
        databaseHandler.removeCallbacks(databaseRunnable);
        updateHandler.removeCallbacks(updateRunnable);
        serverHandler.removeCallbacks(serverRunnable);
        //cancel current transaction
        if (transaction != null && !transaction.isCancelled()) {
            transaction.cancel();
        }

        //destroy managers
        mMotionCustomManager.onDestroy();
        mLocationCustomManager.onDestroy();
        mVariousSensorsCustomManager.onDestroy();
        mWifiCustomManager.onDestroy();
        mBluetoothCustomManager.onDestroy();
        Log.i(LOG_TAG, "Service Destroyed!");
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * asynchronously creates a new Sensor entry object, retrieving data from the sensor managers and uploads it to the database
     */
    private void updateDatabase() {


        if(realm != null)
        {
            transaction = realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realmAsync) {


                    int battery = mVariousSensorsCustomManager.getBatteryLevel();

                    MotionValues motionValues = mMotionCustomManager.extractMotionValues();
                    boolean inMotion = motionValues.isInMotion();
                    float magneticField = mVariousSensorsCustomManager.extractMagneticField();
                    boolean display = mVariousSensorsCustomManager.isDisplayOn();
                    float proximity = mVariousSensorsCustomManager.getProximity();
                    float totalDistance = mLocationCustomManager.extractTotalDistance();
                    RealmList<LocationCustom> locationList = mLocationCustomManager.extractLocationList();
                    boolean moving = mLocationCustomManager.isMoving(totalDistance, locationList, null);
                    RealmList<WifiDeviceCustom> wifiDevices = mWifiCustomManager.extractWifiDevicesList();
                    RealmList<WifiNetworkCustom> wifiNetworks = mWifiCustomManager.extractWifiNetworksList();
                    RealmList<BluetoothDeviceCustom> bluetoothDevices = mBluetoothCustomManager.extractBluetoothDevicesList();
                    String currentNetworkSSID = mWifiCustomManager.getCurrentWifiNetworkSSID();

                    int signalStrength = mVariousSensorsCustomManager.getSignalStrength();
                    long finalTimestamp = System.currentTimeMillis();
                    //calculate maximum speed
                    float max_speed = 0.0f;
                    if(locationList != null)
                        for(int i = 0; i < locationList.size(); i++)
                        {
                            LocationCustom location = locationList.get(i);
                            if (location != null && location.getSpeed() > max_speed) {
                                    max_speed = location.getSpeed();
                            }

                        }


                    //delete duplicate objects to avoid inserting redundant data in the database
                    deleteDuplicates(realmAsync,wifiDevices,wifiNetworks,bluetoothDevices);

                    //set ids
                    long id = nextIdRealm(WifiDeviceCustom.class,realmAsync);
                    if (wifiDevices != null) {
                        for(WifiDeviceCustom wifiDevice:wifiDevices)
                        {
                            if(wifiDevice.getId() == 0)
                            {
                                wifiDevice.setId(id);
                                id++;
                            }

                        }

                    }
                    id = nextIdRealm(WifiNetworkCustom.class,realmAsync);
                    if (wifiNetworks != null) {
                        for(WifiNetworkCustom network:wifiNetworks)
                        {
                            if(network.getId() == 0)
                            {
                                network.setId(id);
                                id++;
                            }

                        }

                    }
                    id = nextIdRealm(BluetoothDeviceCustom.class,realmAsync);
                    if (bluetoothDevices != null) {
                        for(BluetoothDeviceCustom btDevice:bluetoothDevices) {
                            if(btDevice.getId() == 0)
                            {
                                btDevice.setId(id);
                                id++;
                            }

                        }
                    }

                    motionValues.setId(nextIdRealm(MotionValues.class, realmAsync));
                    id = nextIdRealm(LocationCustom.class,realmAsync);
                    if (locationList != null) {

                        for(LocationCustom location:locationList)
                        {
                            if(location.getId() == 0)
                            {
                                location.setId(id);
                                id++;
                            }

                        }

                    }


                    SensorsEntry entry = new SensorsEntry(beginningTime, finalTimestamp, battery, signalStrength, motionValues, inMotion, moving, display, max_speed, totalDistance, currentNetworkSSID, magneticField, proximity, locationList, wifiDevices, wifiNetworks, bluetoothDevices);
                    entry.setId(nextIdRealm(SensorsEntry.class, realmAsync));

                    realmAsync.insertOrUpdate(entry);
                    Log.i(LOG_TAG, "NEW ENTRY" + " " + entry.toString());

                    //update managers shared preferences
                    mLocationCustomManager.updateSharedPreferences();
                    mBluetoothCustomManager.updateSharedPreferences();
                    mWifiCustomManager.updateSharedPreferences();
                    mVariousSensorsCustomManager.updateSharedPreferences();

                    databaseHandler.postDelayed(databaseRunnable, DELAY_DB); //call this method again in 1min
                    beginningTime = System.currentTimeMillis(); //set beginning time for next entry
                }
            });
        }
        else
        {
            databaseHandler.postDelayed(databaseRunnable, DELAY_DB); //call this method again in 1min
        }

    }

    /**
     * checks if there are duplicate objects in the lists and in the database and if there is, replace the data in the list for the data in the database
     * @param realmAsync - Realm object
     * @param wifiDevices - list of wifiDevices
     * @param wifiNetworks - list of wifiNetworks
     * @param bluetoothDevices - list of bluetoothDevices
     */
    private void deleteDuplicates(Realm realmAsync, RealmList<WifiDeviceCustom> wifiDevices, RealmList<WifiNetworkCustom> wifiNetworks, RealmList<BluetoothDeviceCustom> bluetoothDevices) {
        if(wifiDevices != null)
            for(int i = 0; i < wifiDevices.size(); i++)
            {

                WifiDeviceCustom device = wifiDevices.get(i);
                WifiDeviceCustom result = null;
                if (device != null) {
                    result = realmAsync.where(WifiDeviceCustom.class)
                            .equalTo("ip", device.getIp())
                            .equalTo("mac", device.getMac())
                            .equalTo("networkSSID", device.getNetworkSSID())
                            .findFirst(); //check if duplicate
                }

                if(result != null)
                    wifiDevices.set(i, result); //replace

            }


        if(wifiNetworks != null)
            for(int i = 0; i < wifiNetworks.size(); i++)
            {
                WifiNetworkCustom network = wifiNetworks.get(i);
                WifiNetworkCustom result = null;
                if (network != null) {
                    result = realmAsync.where(WifiNetworkCustom.class)
                            .equalTo("SSID", network.getSSID())
                            .equalTo("BSSID", network.getBSSID())
                            .findFirst(); //check if duplicate
                }

                if(result != null)
                    wifiNetworks.set(i, result); //replace
            }

        if(bluetoothDevices != null)
            for(int i = 0; i < bluetoothDevices.size(); i++)
            {
                BluetoothDeviceCustom device = bluetoothDevices.get(i);
                BluetoothDeviceCustom result = null;
                if (device != null) {
                    result = realmAsync.where(BluetoothDeviceCustom.class)
                            .equalTo("address", device.getAddress())
                            .equalTo("name", device.getName())
                            .equalTo("type", device.getType())
                            .findFirst(); //check if duplicate
                }

                if(result != null)
                    bluetoothDevices.set(i, result); //replace
            }


    }

    /**
     * sets energy mode and updates the managers
     */
    private void updateManagers()
    {
        int battery = mVariousSensorsCustomManager.getBatteryLevel();
        boolean InMotion = mMotionCustomManager.extractInMotionRecent();


        int energyMode;
        if(Math.abs(battery) > 50 || battery > 0)
        {
            if(InMotion)
            {
                energyMode = EnergyModes.MODE_HIGH_BATTERY_INMOTION;
                Log.i(LOG_TAG, "Mode: High Battery & In Motion");
            }
            else
            {
                energyMode = EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION;
                Log.i(LOG_TAG, "Mode: High Battery & Not In Motion");
            }

        }
        else{
            if(InMotion)
            {
                energyMode = EnergyModes.MODE_LOW_BATTERY_INMOTION;
                Log.i(LOG_TAG, "Mode: Low Battery & In Motion");
            }
            else
            {
                energyMode = EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION;
                Log.i(LOG_TAG, "Mode: Low Battery & Not In Motion");
            }

        }
        mLocationCustomManager.setModeAndUpdate(energyMode);
        mMotionCustomManager.setModeAndUpdate(energyMode);
        mWifiCustomManager.setModeAndUpdate(energyMode);
        mBluetoothCustomManager.setModeAndUpdate(energyMode);
    }

    /**
     * deletes entries, as well as the locations and motionValues associated
     * @param entriesToDelete - RealmResults containing all the entries to delete
     * @param realm - Realm object
     */
    private void deleteFromDatabase(RealmResults<SensorsEntry> entriesToDelete, Realm realm)
    {
        ArrayList<Long> motionValuesIdsToDelete = new ArrayList<>();
        ArrayList<Long> locationCustomIdsToDelete = new ArrayList<>();
        for(SensorsEntry entry:entriesToDelete)
        {
            RealmList<LocationCustom> locations = entry.getLocationList();
            for(LocationCustom location:locations)
            {
                locationCustomIdsToDelete.add(location.getId());
            }
            motionValuesIdsToDelete.add(entry.getId());
        }
        RealmResults<MotionValues> motionValuesToDelete = realm.where(MotionValues.class).in("id", longListToArray(motionValuesIdsToDelete)).findAll();
        RealmResults<LocationCustom> locationsToDelete = realm.where(LocationCustom.class).in("id", longListToArray(locationCustomIdsToDelete)).findAll();

        entriesToDelete.deleteAllFromRealm();
        motionValuesToDelete.deleteAllFromRealm();
        locationsToDelete.deleteAllFromRealm();

    }

    /**
     * @param clazz - class
     * @param realm - Realm object
     * @return first non-taken id from a certain class
     */
    private long nextIdRealm(Class clazz, Realm realm)
    {
        Number currentIdNum = realm.where(clazz).max("id");
        long nextId;
        if(currentIdNum == null) {
            nextId = 1;
        } else {
            nextId = currentIdNum.longValue() + 1;
        }
        return nextId;
    }

    //converts a list of Long to an array
    private static Long[] longListToArray(List<Long> longs)
    {
        Long[] ret = new Long[longs.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = longs.get(i);
        }
        return ret;
    }

    /**
     * @return true if device is connected to the internet, false if is not
     */
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }


    /**
     * sends an entry to the server as a string
     * @param url server url
     * @param entry entry to send
     * @return true if successful false if not
     */
    private static boolean POST(String url, SensorsEntry entry){

        InputStream inputStream = null;
        boolean result = false;
        if(url != null)
            try {

                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(url);

                /*String json = "";

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("name", person.getName());
                jsonObject.accumulate("country", person.getCountry());
                jsonObject.accumulate("twitter", person.getTwitter());

                // 4. convert JSONObject to JSON to String
                json = jsonObject.toString();

                // ** Alternative way to convert Person object to JSON string using Jackson Lib
                // ObjectMapper mapper = new ObjectMapper();
                // json = mapper.writeValueAsString(person);

                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);*/

                // 6. set httpPost Entity
                httpPost.setEntity(new StringEntity(entry.toString()));

                // 7. Set some headers to inform server about the type of the content
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);

                // 9. receive response as inputStream
                inputStream = httpResponse.getEntity().getContent();

                // 10. convert inputstream to string
                if(inputStream != null)
                    result = true;//convertInputStreamToString(inputStream);
                else
                    result = false;//"Did not work!";

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }

        // 11. return result
        return result;
    }








}
