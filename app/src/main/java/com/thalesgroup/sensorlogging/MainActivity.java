package com.thalesgroup.sensorlogging;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {



    private final int MY_PERMISSIONS_REQUEST = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ask for permissions
        String[] allPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET,
                Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED};

        ArrayList<String> permissionsToAsk = new ArrayList<>();
        for(String permission:allPermissions)
        {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsToAsk.add(permission);

        }

        if(!permissionsToAsk.isEmpty())
            ActivityCompat.requestPermissions(this, Arrays.copyOf(permissionsToAsk.toArray(), permissionsToAsk.size(), String[].class), MY_PERMISSIONS_REQUEST);


        //check if isScanAlwaysAvailable is enabled, if not, ask for it
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !wifiManager.isScanAlwaysAvailable()) {

            startActivity(new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE));
        }

        //send a broadcast for ServiceRestarterBroadcastReceiver
        Intent broadcastIntent = new Intent("com.thalesgroup.sensorlogging.ServiceRestarterBroadcastReceiver");
        sendBroadcast(broadcastIntent);



    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        if(requestCode == MY_PERMISSIONS_REQUEST)
        {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
            }
            else
            {
                //permission denied
            }
        }

    }



}
