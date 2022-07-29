# SensorLogging-Android
 Android App to collect and analyze smartphone sensor data. Developed in Java using Android Studio

## Objective
The objective of this project was to develop an application for the Android system that collects data from the device's sensors and saves them in a database, to then be sent to a server and processed. One of the concerns when developing the application will be to minimize battery consumption.

## Data Collection
Data collection is done through a service (DataAquisitionService). The goal is for this service to be running constantly. For this purpose, a BroadcastReceiver (ServiceRestarterBroadcastReceiver) was created, whose function is to start the service if it is not running. This BroadcastReceiver is triggered in several situations:

1. When the device is turned on (boot);

2. When the application is opened;

3. Immediately after the service is completed;

4. When a change in connectivity status is detected;

5. Every two minutes, through the AlarmManager class (whenever the Receiver is triggered, it produces an intent to be triggered again in two minutes).
The reason why situations 1, 2 and 3 are not enough for the service to run constantly is because the BroadcastReceiver is not always activated when the service is terminated: for this to happen, the service must be terminated and its onDestroy() method must be executed, which almost always doesn't happen because the Android system, in low memory situations, stops services without letting them execute their onDestroy() method.

(NOTE: there are still situations where the service is not running: for example, situations where the Android system enters a “sleep” state and postpones the alarms created by the AlarmManager class).

This service contains five Managers whose function is to collect and store the necessary data until the service extracts it. These Managers are:

• BluetoothCustomManager – Generate and scan bluetooth devices automatically and when relevant.

• WifiCustomManager – Generate and scan visible wifi networks and scan devices connected to the network automatically and when relevant.

• MotionCustomManager – Generate the device's motion sensors.

• LocationCustomManager – Generate location sensors.

• VariousSensorsCustomManager – Manage the remaining sensors/features.

Every minute (DELAY_DB), the DataAquisitionService service extracts the data from the Managers, that is, it collects the data collected by them since the last extraction.
(NOTE: the time interval between two consecutive extracts may exceed the value of DELAY_DB, for example in situations where the Android system is low on memory).

## Data Collected
The data that can be extracted from each manager, and that are grouped in each extraction in an object of the SensorsEntry class, are the following:

### BluetoothCustomManager:
#### bluetoothDevices (RealmList<BluetoothDeviceCustom>) – list of visible bluetooth devices. If more than one scan has been performed between two consecutive extractions, this information is only relative to the last scan performed. The BluetoothDeviceCustom class has the following fields:
##### BluetoothDeviceCustom:
###### address (String) - mac address of the Bluetooth device.
###### name (String) - name of the Bluetooth device.
###### type (int) - bluetooth device type (1 = Classic - BR/EDR devices, 2 = Dual Mode - BR/EDR/LE, 3 = Low Energy - LE-only, 0 = Unknown).
#### numberBluetoothDevices (int) - size of the list above or –1 if there is no information (no scans have been performed since the last extraction).
### WiFiCustomManager:
#### wifiDevices (RealmList<WifiDeviceCustom>) – list of devices on the network with mac address (including routers). If more than one scan has been performed between two consecutive extractions, this information is only relative to the last scan performed. The WifiDeviceCustom class has the following fields:
##### WiFiDeviceCustom:
###### ip (String) - ip address of the device on the network.
###### mac(String) - device's mac address.
###### networkSSID – SSID of the network where the device was seen.
##### numberWifiDevices (int) - size of the list above or –1 if there is no information (no scans have been performed since the last extraction).
#### wifiNetworks (RealmList<WifiNetworkCustom>) – list of visible wifi networks. If more than one scan has been performed between two consecutive extractions, this information is only relative to the last scan performed. The WifiNetworkCustom class has the following fields:
##### WiFiNetworkCustom:
###### SSID (String) - SSID of the network.
###### BSSID (String) - BSSID address of the network.
#### numberWifiNetworks (int) - size of the list above or –1 if there is no information (no scans have been performed since the last extraction).
#### currentNetworkSSID (String) - SSID of the wifi network we are currently connected to. (This reading is taken only at the time of extraction).
### MotionCustomManager:
#### motionValues (MotionValues) - data structure that contains information resulting from data from the device's motion sensors (accelerometer, linear acceleration, gravity...), collected since the last extraction. This structure has the following fields:
##### MotionValues:
###### averageAcceleration (float) - average of the modules of the linear acceleration vectors (m/s<sup>2</sup>) (NOTE: in devices that do not have a linear acceleration sensor or a gravity sensor (most devices), that is, have only an accelerometer, the vector linear acceleration is calculated by passing the values obtained by the accelerometer through a low-pass filter to remove gravity, which causes the obtained linear acceleration values to be subject to greater error).
###### standardDeviationAcceleration (float) - standard deviation of the magnitudes of linear acceleration vectors (m/s<sup>2</sup>).
###### averageVelocity (float) - average of the magnitudes of the velocity vectors (m/s) (NOTE: the velocity vector is calculated by “integrating” the linear acceleration vector, which does not give very accurate results, since we are integrating a discrete set If, in addition, the device only has an accelerometer, we will increase the error even more. For this reason, it is advisable to avoid using the averageVelocity field, it is preferable to use the “speed” field given by the location, if it is available).
###### standardDeviationVelocity (float) - standard deviation of velocity vector modules (m/s).
###### averageInclinationX (float) - average device inclination relative to the x axis (degrees).
###### standardDeviationInclinationX (float) - standard deviation of the device's inclination relative to the y-axis (degrees).
###### averageInclinationY (float) - average device inclination relative to the x axis (degrees).
###### standardDeviationInclinationY (float) - standard deviation of the device's inclination relative to the y-axis (degrees).
###### inMotion (boolean) - indicates if the device remained at rest (false) or if there was at least some movement (movement exists if the inclination in any of the axes varied by more than 3 degrees) (true). In practice, this field is false only when the device is resting on any surface, or in certain cases when the user is sitting with the device in his pocket.
 
![image](https://user-images.githubusercontent.com/60743836/181654990-1057bf0c-a60e-487a-9967-7394ad2dfb07.png)


### LocationCustomManager:
#### locationList (RealmList<LocationCustom>) - list of locations obtained by location services since the last extraction. The LocationCustom class has the following fields:
##### LocationCustom:
###### provider (String) - location provider (“gps”/”network”).
###### latitude (double) - latitude (in degrees).
###### longitude (double – longitude (in degrees).
###### altitude (double) - altitude (in meters), or 0.0 if not available.
###### bearing (float) - direction (in degrees) in range (0.0, 360.0] or 0.0 if not available.
###### speed (float) - speed (in m/s) or 0.0 if not available.
###### accuracy (float) - horizontal location accuracy in meters (circle radius with 68% confidence).
###### numberOfSatellites (int) - number of satellites that contributed to the location (in case the provider is “gps” or 0 if it is “network”).
###### timestamp (long) - time when the measurement took place (in milliseconds).
#### totalDistance (float) - estimate of the total distance (in meters) traveled since the moment corresponding to the last location found before the last extraction. Calculated by summing all distances between consecutive locations. (NOTE: this value does not represent the distance traveled since the last extraction, but the distance traveled since the last location update before the last extraction, which is important to take into account in situations where there are no location updates for some time and suddenly begin to exist (for example when you leave the subway)).
#### moving (boolean) - indicates if the location has remained practically constant since the last extraction (false) or if there have been enough changes to consider that the device has been in motion (for example the user has been walking or riding a transport or on foot) (true).

### VariousSensorsCustomManager:
#### batteryLevel (int) - the absolute value of this field indicates the device's battery percentage. If the device is charging, the value is positive, otherwise it is negative. (This reading is taken only at the time of extraction).
#### display (boolean) - indicates whether the device screen is on (true) or off (false). (This reading is taken only at the time of extraction).
#### proximity (float) - distance in cm that an object is from the device's proximity sensor. (NOTE: on some devices this reading is binary i.e. 8 = far, 0 = close) (This reading is only taken at the time of extraction).
#### signalStrength (int) - strength of the mobile network signal in dBm. (This reading is taken only at the time of extraction).
#### magneticField (float) - average magnetic field strength since the last extraction (in µT).

In this object of the SensorEntry class, the following fields are still present:
#### maxSpeed (float) - maximum speed (in m/s) reached by the device, based on the locations obtained.
#### beginningTimestamp (long) and finalTimestamp (long) - starting and ending time instants, respectively, within which measurements were made.
#### onServer (boolean) - indicates whether this entry was introduced on the server (true) or not (false).

## Operating Modes
Managers have 4 working modes (present in the EnergyModes abstract class):

• MODE_HIGH_BATTERY_INMOTION: device in motion (inMotion) and battery percentage greater than 50% or device is charging.

• MODE_HIGH_BATTERY_NOT_INMOTION: device stopped (!inMotion) and battery percentage greater than 50% or device is charging.

• MODE_LOW_BATTERY_INMOTION: device in motion (inMotion) and battery percentage lower than 50%, without charging.

• MODE_LOW_BATTERY_NOT_INMOTION: device stopped (!inMotion) and battery percentage lower than 50%, without charging.

Depending on the mode the device is in, managers behave differently:

• BluetoothCustomManager: The minimum time between two scans of bluetooth devices depends on the mode:

o MODE_HIGH_BATTERY_INMOTION: 2 minutes

o MODE_HIGH_BATTERY_NOT_INMOTION: 20 minutes

o MODE_LOW_BATTERY_INMOTION: 5 minutes

o MODE_LOW_BATTERY_NOT_INMOTION: 1 hour

• WiFiCustomManager: The minimum time between two scans of wifi networks depends on the mode:

o MODE_HIGH_BATTERY_INMOTION: 2 minutes

o MODE_HIGH_BATTERY_NOT_INMOTION: 20 minutes

o MODE_LOW_BATTERY_INMOTION: 5 minutes

o MODE_LOW_BATTERY_NOT_INMOTION: 1 hour

The minimum time between two consecutive scans of devices on the network is taken when the device connects to a different network than it was previously connected to or, staying on the same network, the minimum time interval between two consecutive scans is as follows:

o MODE_HIGH_BATTERY_INMOTION: 5 minutes

o MODE_HIGH_BATTERY_NOT_INMOTION: 30 minutes

o MODE_LOW_BATTERY_INMOTION: 10 minutes

o MODE_LOW_BATTERY_NOT_INMOTION: 1 hour

• MotionCustomManager:

Although this value is just a suggestion and there is no guarantee that Android will respect it, the time interval between consecutive values ​​from the motion sensors depends on the mode:

o MODE_HIGH_BATTERY_INMOTION: 0.2 seconds

o MODE_HIGH_BATTERY_NOT_INMOTION: 0.5 seconds

o MODE_LOW_BATTERY_INMOTION: 1 second

o MODE_LOW_BATTERY_NOT_INMOTION: 2 seconds

This manager always uses the accelerometer to calculate the slope, but to calculate the linear acceleration (and consequently the speed), the first set of sensors available on the device is used, among the following:

1. TYPE_LINEAR_ACCELERATION (linear acceleration sensor)

2. TYPE_GRAVITY + TYPE_ACCELEROMETER (gravity and accelerometer sensors)

3. TYPE_ACCELEROMETER (accelerometer)

• LocationCustomManager:

There are three location providers in the Android system. They are: passive, network and gps, in ascending order of battery consumption. Of these three, the first one (that is available) is chosen to be the primary. The primary provider receives location updates at a (minimum) interval defined by the mode. In addition to the primary one, the application uses two auxiliary providers: auxiliary provider 1 (network) and auxiliary provider 2 (gps), which receive location updates immediately after they are available (as soon as possible).

o MODE_HIGH_BATTERY_INMOTION: Primary provider always active, with minimum interval between updates of 5 seconds. Auxiliary Provider 1 is activated if there has not been a location update for more than 30 seconds and has been off for more than 30 seconds, and is turned off immediately after a location update is received or within 30 seconds of being turned on. Auxiliary Provider 2 is activated if there has been no location update for more than 45 seconds and has been off for more than 45 seconds, and is turned off immediately after receiving a location update or within 15 seconds of being turned on.

o MODE_HIGH_BATTERY_NOT_INMOTION: Same as MODE_HIGH_BATTERY_INMOTION during the first 5 minutes. After this time, all providers are turned off (idle state).

o MODE_LOW_BATTERY_INMOTION: Primary provider always active, with minimum interval between updates of 10 seconds. Auxiliary Provider 1 is activated if there has been no location update for more than 90 seconds and has been off for more than 90 seconds, and is turned off immediately after a location update is received or within 30 seconds of being turned on. Auxiliary Provider 2 is activated if there has not been a location update for more than 105 seconds and has been off for more than 105 seconds, and it is turned off immediately after a location update is received or within 15 seconds of being turned on.

o MODE_LOW_BATTERY_NOT_INMOTION: Same as MODE_LOW_BATTERY_INMOTION during the first 5 minutes. After this time, all providers are turned off (idle state).
There is also a moving state, which is activated whenever the user reaches a considerable speed (approximately walking speed) or a significant change in the user's location is detected in the last 3 minutes (or more, if there is no information about the location in the last 3 minutes). 3 minutes). In this state, there is always at least one of the auxiliary providers constantly connected, and both can be connected if it is determined that only one is not enough.

• VariousSensorsCustomManager:

The operation of this manager is independent of the device's mode.

## Database and Server

The database used to store the extractions is called Realm. Objects of classes SensorEntry, BluetoothDeviceCustom, LocationCustomManager, MotionValues, WifiDeviceCustom and WifiNetworkCustom are stored in the database, and before each insertion a check is made to ensure that there are no duplicate objects of classes BluetoothDeviceCustom, WifiDeviceCustom and WifiNetworkCustom. Every hour, if there is a wifi connection, the entries present in the database are sent to the server and eliminated, except for the objects of the BluetoothDeviceCustom, WifiDeviceCustom and WifiNetworkCustom classes.

## Future Work

One of the aspects that will have to be changed in this application is the way in which wifi networks are scanned. At the moment this scan is done by the WifiCustomManager class using the WifiManager.startScan() method, which is discontinued. As there is currently no other alternative to perform this operation, the only option is to use this method.
There are at least two features that have not yet been tested: the acquisition of the magneticField field (the tester did not have this sensor) and the acquisition of the signalStrength field on devices with Android version older than JELLY_BEAN_MR1.
One of the aspects to improve will be to develop a method to prevent the Android system from stopping the service for long periods of time. One idea is to use, in the ServiceRestarterBroadcastReceiver class, instead of the AlarmManager.set(…) method, the AlarmManager.setAndAllowWhileIdle(…) method, to define the alarms that will activate the Receiver, and in this way the battery consumption will be higher .
There is a method implemented in the DataAquisitionService class that allows you to send all entries in the database in the form of a String to a url (SERVER_URL, which currently has no address) and later eliminate them, however this method has not yet been tested and will have to be tweaked to work with the server.

## How to use

Just extract the SensorLogging.zip file. This will create a folder called SensorLogging where the entire project is. Using Android Studio software (version 3.1.4), it is possible to open this folder as a project and edit the files or install the application on a device. All the classes mentioned above can be found in the SensorLogging\app\src\main\java\com\thalesgroup\sensorlogging folder.
To view the contents of the database, simply open the file that is located on the Android device at /data/data/com.thalesgroup.sensorlogging/files/default.realm with the RealmStudio software.
