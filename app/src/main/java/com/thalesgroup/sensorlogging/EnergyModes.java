package com.thalesgroup.sensorlogging;

/*
  Created by thales on 27/07/2018.
 */

/**
 * abstract class containing integers that represent modes in which the device can be
 */
public abstract class EnergyModes {
    public static final int MODE_LOW_BATTERY_INMOTION = 1;
    public static final int MODE_LOW_BATTERY_NOT_INMOTION = 2;
    public static final int MODE_HIGH_BATTERY_INMOTION = 3;
    public static final int MODE_HIGH_BATTERY_NOT_INMOTION = 4;
}
