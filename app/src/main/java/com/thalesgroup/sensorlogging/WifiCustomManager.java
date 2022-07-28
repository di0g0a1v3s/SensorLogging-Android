package com.thalesgroup.sensorlogging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.realm.RealmList;

/*
 * Created by thales on 26/07/2018.
 */

/**
 * Manages and performs scans on wifi networks and devices on the networks the device is currently in.
 * Does this automatically when relevant. This data can be extracted through the methods
 * extractWifiNetworksList() and extractWifiDevicesList(). Requires the method setModeAndUpdate(int mode)
 * to be called every 10 secs or so to make the necessary updates.
 */
public class WifiCustomManager {


    private static final long TEN_MINUTES = 10*60*1000;
    private static final long TWO_MINUTES = 2*60*1000;
    private static final long TWENTY_MINUTES = 20*60*1000;
    private static final long THIRTY_MINUTES = 30*60*1000;
    private static final long FIVE_MINUTES = 5*60*1000;
    private static final long ONE_HOUR = 60*60*1000;
    private static final int NB_THREADS = 128; //number of threads for pinging other devices on network
    private List<ScanResult> currentWifiNetworksVisible = null;
    private List<WifiDeviceCustom> currentWifiDevicesVisible = null;
    private int mode = -1; //EnergyMode
    private final WifiManager mWifiManager;
    private final Context mContext; //ApplicationContext
    private static final String LOG_TAG = "WifiCustomManager";
    private final SharedPreferences sharedPref;

    private String latestWifiNetworkSSID = null;
    private final static String SHARED_PREF_LATEST_SSID = "com.thalesgroup.sensorlogging.WifiCustomManager.latestWifiNetworkSSID";
    private long timeOfLastWifiNetworksScan = 0;
    private final static String SHARED_PREF_TIME_LAST_SCAN_NETWORKS = "com.thalesgroup.sensorlogging.WifiCustomManager.timeOfLastWifiNetworksScan";
    private long timeOfLastWifiDevicesScan = 0;
    private final static String SHARED_PREF_TIME_LAST_SCAN_DEVICES = "com.thalesgroup.sensorlogging.WifiCustomManager.timeOfLastWifiDevicesScan";

    //Broadcast receiver for wifi scan results available intents
    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        /**
         * called when the wifi scan for networks has finished. Populates the currentWifiNetworksVisible with the networks
         * @param c - App context
         * @param intent
         */
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                currentWifiNetworksVisible = mWifiManager.getScanResults();

                Log.i(LOG_TAG, "...wifi networks scan finished. " + currentWifiNetworksVisible.size() + " networks found.");
                timeOfLastWifiNetworksScan = System.currentTimeMillis();

            }
        }
    };


    /**
     * Constructor
     * @param mContext - Application Context
     */
    public WifiCustomManager(Context mContext) {

        this.mContext = mContext;

        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mContext.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        sharedPref = mContext.getSharedPreferences(DataAcquisitionService.SHARED_PREF_TAG, Context.MODE_PRIVATE);
        latestWifiNetworkSSID = sharedPref.getString(SHARED_PREF_LATEST_SSID, null);
        timeOfLastWifiNetworksScan = sharedPref.getLong(SHARED_PREF_TIME_LAST_SCAN_NETWORKS, 0);
        timeOfLastWifiDevicesScan = sharedPref.getLong(SHARED_PREF_TIME_LAST_SCAN_DEVICES, 0);

    }

    /**
     * updates in the shared preferences file the values of the variables stored
     */
    public void updateSharedPreferences() {

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_LATEST_SSID, latestWifiNetworkSSID);
        editor.putLong(SHARED_PREF_TIME_LAST_SCAN_NETWORKS, timeOfLastWifiNetworksScan);
        editor.putLong(SHARED_PREF_TIME_LAST_SCAN_DEVICES, timeOfLastWifiDevicesScan);
        editor.apply();
    }

    /**
     * Update energy mode and start the scanning of wifi networks and devices if necessary
     * @param mode - current EnergyMode of the device
     */
    public void setModeAndUpdate(int mode) {

        if(mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_INMOTION || mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION)
        {
            this.mode = mode;
        }


        if(shouldScanWifiNetworks())
            scanWifiNetworks();

        if(shouldScanWifiDevices())
            scanWifiDevices();


    }

    /**
     * Returns and clears the list of currently available wifi networks visible
     * @return list of currently available wifi networks visible or null
     */
    @Nullable
    public RealmList<WifiNetworkCustom> extractWifiNetworksList()
    {
        List<ScanResult> list = currentWifiNetworksVisible;
        currentWifiNetworksVisible = null;
        if(list == null)
            return null;
        RealmList<WifiNetworkCustom> realmList = new RealmList<>();
        for(ScanResult r:list)
            realmList.add(new WifiNetworkCustom(r));

        return realmList;
    }

    /**
     * Returns and clears the list of devices in the network
     * @return list of devices in the network or null
     */
    @Nullable
    public RealmList<WifiDeviceCustom> extractWifiDevicesList()
    {
        List<WifiDeviceCustom> list = currentWifiDevicesVisible;
        currentWifiDevicesVisible = null;
        if(list == null)
            return null;
        RealmList<WifiDeviceCustom> realmList = new RealmList<>();
        realmList.addAll(list);
        return realmList;
    }

    /**
     * determines whether it's relevant or not to scan for wifi networks
     * @return true = should scan, false = shouldn't scan
     */
    private boolean shouldScanWifiNetworks()
    {
        //if wifi is not enabled, don't scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(!(isWifiOn() || mWifiManager.isScanAlwaysAvailable()))
                return false;
        }
        else
        {
            if(!isWifiOn())
                return false;
        }
        //if there's already data, don't scan
        if(currentWifiNetworksVisible != null)
            return false;


        //if enough time has passed that it becomes relevant to scan again, scan
        if(mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastWifiNetworksScan > TWO_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastWifiNetworksScan > TWENTY_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastWifiNetworksScan > FIVE_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastWifiNetworksScan > ONE_HOUR)
            return true;

        return false;

    }

    /**
     * determines whether it's relevant or not to scan for devices on the network
     * @return true = should scan, false = shouldn't scan
     */
    private boolean shouldScanWifiDevices()
    {

        //if the device isn't connected to a wifi network, don't scan
        if(!isWifiConnected())
        {
            latestWifiNetworkSSID = null;
            return false;
        }
        String currentWifiNetwork = getCurrentWifiNetworkSSID();
        //if the device has changed the network it was connected to, scan
        if(latestWifiNetworkSSID == null || !latestWifiNetworkSSID.equals(currentWifiNetwork))
        {
            latestWifiNetworkSSID = currentWifiNetwork;
            return true;
        }
        //if there's already data, don't scan
        if(currentWifiDevicesVisible != null)
            return false;

        //if enough time has passed that it becomes relevant to scan again, scan
        if(mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastWifiDevicesScan > FIVE_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastWifiDevicesScan > THIRTY_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_INMOTION && System.currentTimeMillis() - timeOfLastWifiDevicesScan > TEN_MINUTES)
            return true;
        if(mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION && System.currentTimeMillis() - timeOfLastWifiDevicesScan > ONE_HOUR)
            return true;

        return false;

    }

    /**
     * @return null or a string containing the SSID of the wifi network we're currently connected to
     */
    @Nullable
    public String getCurrentWifiNetworkSSID() {
        String networkSSID = null;
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (cm != null) {
            networkInfo = cm.getActiveNetworkInfo();
        }
        if (networkInfo == null) {
            return null;
        }

        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo;
            if (wifiManager != null) {
                connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                    networkSSID = connectionInfo.getSSID();
                    CharacterIterator it = new StringCharacterIterator(networkSSID);
                    if(it.first() == '"' && it.last() == '"')
                    {
                        networkSSID = networkSSID.substring(1, networkSSID.length() - 1);
                    }

                }
            }

        }

        return networkSSID;
    }


    /**
     * clears the list of current wifi networks visible and starts a new scan
     */
    public void scanWifiNetworks()
    {
        timeOfLastWifiNetworksScan = System.currentTimeMillis();
        currentWifiNetworksVisible = null;
        Log.i(LOG_TAG, "Wifi networks scan started...");
        mWifiManager.startScan();
    }

    /**
     * clears the list of current wifi devices on the network and performs a new scan
     */
    private void scanWifiDevices()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG, "Wifi devices scan started...");
                currentWifiDevicesVisible = new ArrayList<>();
                doScanWifiDevices();
                readAddressesWifiDevices();
                timeOfLastWifiDevicesScan = System.currentTimeMillis();
                if(currentWifiDevicesVisible != null)
                    Log.i(LOG_TAG, "...wifi devices scan finished. " + currentWifiDevicesVisible.size() + " devices found.");
            }
        }).start();

    }


    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    /**
     * reads the /proc/net/arp file to extract the ip and mac addresses of all the devices in the network and adds them to
     * the currentWifiDevicesVisible list
     */
    private void readAddressesWifiDevices() {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (mac != null && mac.matches("..:..:..:..:..:..") && !mac.equals("00:00:00:00:00:00")) {
                        String currentNetworkSSID = getCurrentWifiNetworkSSID();
                        WifiDeviceCustom thisDevice = new WifiDeviceCustom(ip, mac, currentNetworkSSID);
                        if(currentWifiDevicesVisible != null && !currentWifiDevicesVisible.contains(thisDevice))
                            currentWifiDevicesVisible.add(thisDevice);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * pings all the addresses 0-255 int he network so that all the devices get listed in the /proc/net/arp file
     */
    private void doScanWifiDevices() {

        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        String myIp = getIPAddress(true);
        String IpWithNoFinalPart  = myIp.replaceAll("(.*\\.)\\d+$", "$1");

        for(int dest=0; dest<255; dest++) {
            String host = IpWithNoFinalPart + dest;
            executor.execute(pingRunnable(host));
        }

        executor.shutdown();
        try { executor.awaitTermination(60*1000, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { }

    }


    /**
     * @param host IP address
     * @return Runnable for pinging an IP address
     */
    private Runnable pingRunnable(final String host) {
        return new Runnable() {
            public void run() {
                try {
                    InetAddress inet = InetAddress.getByName(host);
                    boolean reachable = inet.isReachable(3000);
                } catch (UnknownHostException e) {
                } catch (IOException e) {
                }
            }
        };
    }


    /**
     * Determines whether the device has wifi enabled
     * @return true if enable, false if not
     */
    private boolean isWifiOn()
    {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * Determines whether the device is connected to a wifi network
     * @return true if connected, false if not connected
     */
    public boolean isWifiConnected() {

        if (mWifiManager.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

            return wifiInfo.getNetworkId() != -1;
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void onDestroy()
    {
        mContext.unregisterReceiver(mWifiScanReceiver);

    }


}
