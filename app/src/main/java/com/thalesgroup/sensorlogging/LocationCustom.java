package com.thalesgroup.sensorlogging;

import android.location.Location;


import java.sql.Timestamp;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
  Created by thales on 01/08/2018.
 */

/**
 * represents a location
 */
public class LocationCustom extends RealmObject {

    @PrimaryKey
    private long id;

    private double latitude;
    private double longitude;
    private double altitude;
    private float bearing;
    private float speed;
    private float accuracy;
    private int numberOfSatellites;
    private String provider;
    private long timestamp;

    public LocationCustom() {
    }

    public LocationCustom(Location location) {
        if(location != null)
        {
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            this.altitude = location.getAltitude();
            this.bearing = location.getBearing();
            this.speed = location.getSpeed();
            this.accuracy = location.getAccuracy();
            this.numberOfSatellites = location.getExtras().getInt("satellites");
            this.provider = location.getProvider();
            this.timestamp = location.getTime();
        }
        else
        {
            this.latitude = Double.NaN;
            this.longitude = Double.NaN;
            this.altitude = Double.NaN;
            this.bearing = Float.NaN;
            this.speed = Float.NaN;
            this.accuracy = Float.NaN;
            this.numberOfSatellites = -1;
            this.provider = "";
            this.timestamp = (long) -1;
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    public void setNumberOfSatellites(int numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return "LocationCustom{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", bearing=" + bearing +
                ", speed=" + speed +
                ", accuracy=" + accuracy +
                ", numberOfSatellites=" + numberOfSatellites +
                ", provider='" + provider + '\'' +
                ", timestamp='" + new Timestamp(timestamp).toString() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationCustom that = (LocationCustom) o;

        if (Double.compare(that.latitude, latitude) != 0) return false;
        if (Double.compare(that.longitude, longitude) != 0) return false;
        if (Double.compare(that.altitude, altitude) != 0) return false;
        if (Float.compare(that.bearing, bearing) != 0) return false;
        if (Float.compare(that.speed, speed) != 0) return false;
        if (Float.compare(that.accuracy, accuracy) != 0) return false;
        if (numberOfSatellites != that.numberOfSatellites) return false;
        if (timestamp != that.timestamp) return false;
        return provider != null ? provider.equals(that.provider) : that.provider == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (bearing != +0.0f ? Float.floatToIntBits(bearing) : 0);
        result = 31 * result + (speed != +0.0f ? Float.floatToIntBits(speed) : 0);
        result = 31 * result + (accuracy != +0.0f ? Float.floatToIntBits(accuracy) : 0);
        result = 31 * result + numberOfSatellites;
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }




}
