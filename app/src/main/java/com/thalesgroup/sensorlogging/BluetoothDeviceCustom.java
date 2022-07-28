package com.thalesgroup.sensorlogging;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
  Created by thales on 01/08/2018.
 */

/**
 * represents a device that is equipped with bluetooth technology
 */
public class BluetoothDeviceCustom extends RealmObject {

    @PrimaryKey
    private long id; //identifier (primary key in database)
    private String address; //mac address of bluetooth device
    private String name; //name of bluetooth device
    private int type; //type of bluetooth device ( 1 = Classic - BR/EDR devices , 2 = Dual Mode - BR/EDR/LE , 3 = Low Energy - LE-only , 0 = Unknown )

    /**
     * Constructor
     * @param device - BluetoothDevice to be converted to BluetoothDeviceCustom
     */
    public BluetoothDeviceCustom(BluetoothDevice device) {
        this.address = device.getAddress();
        this.name = device.getName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.type = device.getType();
        }
        else
        {
            this.type = 0;
        }
    }

    /**
     * Empty constructor
     */
    public BluetoothDeviceCustom() {
    }

    //------Setters and getters-----------
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return "BluetoothDeviceCustom{" +
                "address='" + address + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    /**
     * two devices are the same if they have the same address, name and type
     * @param o object to be compared to
     * @return true if equal, false if not
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BluetoothDeviceCustom that = (BluetoothDeviceCustom) o;

        if (type != that.type) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + type;
        return result;
    }

}
