package com.thalesgroup.sensorlogging;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/*
 Created by thales on 19/07/2018.
 */

/**
 * Manages motion sensors such as the accelerometer and uses them to calculate things such as
 * device inclination, acceleration, velocity.
 * Does this automatically. This data can be extracted through the method extractMotionValues().
 * Requires the method setModeAndUpdate(int mode)
 * to be called every 10 secs or so to make the necessary updates.
 */
public class MotionCustomManager implements SensorEventListener {

    private static final int LIN_ACC = 1;
    private static final int GRAV_and_ACC = 2;
    private static final int ACC = 3;
    private static final int NONE = 8;
    private static final int MIN_INCLINATION_FOR_MOTION = 3; //degrees



    private final int AccelerationAndGravityAcquisitionMode;
    private final SensorManager mSensorManager;
    private int mode; //EnergyMode
    private int sensors_delay; //delay between sensor updates (us)
    private float[] currentAcceleration = new float[3]; //acceleration on x, y and z axis
    private float[] currentGravity = new float[3]; //gravity on x, y and z axis
    private float[] currentVelocity = new float[3]; //velocity on x, y and z axis
    private long instantOfLastAccelerationUpdateNanoseconds = -1;
    private boolean significantMotionRecent = false;
    private boolean motionDetectRecent = false;

    private List<float[]> inclinationList = new ArrayList<>();
    private List<float[]> inclinationListRecent = new ArrayList<>();
    private List<float[]> accelerationList = new ArrayList<>();
    private List<float[]> velocityList = new ArrayList<>();

    private boolean hasBeenInMotion = false;


    /**
     * Constructor
     * @param mContext Application Context
     */
    public MotionCustomManager(Context mContext) {
        this.mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        this.AccelerationAndGravityAcquisitionMode = determineAccelerationAndGravityAcquisitionMode();
    }

    /**
     * Determines which mode to use to collect gravity and acceleration data based on the sensors that the device has
     * @return mode
     */
    private int determineAccelerationAndGravityAcquisitionMode()
    {
        if(getSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null && getSensor(Sensor.TYPE_GRAVITY) != null)
        {
            return LIN_ACC;
        }
        else if(getSensor(Sensor.TYPE_GRAVITY) != null && getSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            return GRAV_and_ACC;
        }
        else if(getSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            return ACC;
        }
        return NONE;

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
     * calculates a new value for the gravity based on the sensor fired and the last known gravity value
     * @param event SensorEvent from the fired sensor
     * @param currentGravity last known gravity value
     * @return new value for the gravity
     */
    @Nullable
    private float[] calculateNewGravity(SensorEvent event, float[] currentGravity)
    {
        float[] newGravity = new float[3];
        if(event.sensor == null || AccelerationAndGravityAcquisitionMode == NONE)
        {
            return null;
        }
        else if(AccelerationAndGravityAcquisitionMode == GRAV_and_ACC && event.sensor.getType() == Sensor.TYPE_GRAVITY)
        {
            System.arraycopy(event.values, 0, newGravity, 0, 3);
        }
        else if(AccelerationAndGravityAcquisitionMode == LIN_ACC && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            for(int i = 0; i <= 2; i++)
                newGravity[i] = 0;
        }
        else if(AccelerationAndGravityAcquisitionMode == ACC && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            final float alpha = 0.4f;

            //Isolate the force of gravity with the low-pass filter.
            for(int i = 0; i <= 2; i++)
                newGravity[i] = alpha * currentGravity[i] + (1 - alpha) * event.values[i];

        }
        else
        {
            return null;
        }

        return newGravity;
    }

    /**
     * calculates a new value for the linear acceleration based on the sensor fired and the last known gravity value
     * @param event SensorEvent from the fired sensor
     * @param currentGravity last known gravity value
     * @return new value for the linear acceleration
     */
    @Nullable
    private float[] calculateNewAcceleration(SensorEvent event, float[] currentGravity)
    {
        float[] newAcceleration = new float[3];
        if(event.sensor == null || AccelerationAndGravityAcquisitionMode == NONE)
        {
            return null;
        }
        else if(AccelerationAndGravityAcquisitionMode == LIN_ACC && event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            System.arraycopy(event.values, 0, newAcceleration, 0, 3);
        }
        else if((AccelerationAndGravityAcquisitionMode == GRAV_and_ACC || AccelerationAndGravityAcquisitionMode == ACC)
                && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            for(int i = 0; i <= 2; i++)
                newAcceleration[i] = event.values[i] - currentGravity[i];
        }
        else
        {
            return null;
        }
        return newAcceleration;
    }

    /**
     * calculates a new value for the inclination of the device based on the raw value from the accelerometer
     * @param accelerometerRaw raw value from the accelerometer
     * @return new value for the inclination of the device
     */
    private float[] calculateNewInclination(float accelerometerRaw[])
    {
        final float[] I_matrix = new float[16];
        final float[] R_matrix = new float[16];
        final float[] outR = new float[16];
        final float[] inclination_rad = new float[3];
        float[] inclination = new float[2];
        float[] geomagnetic = new float[] {1f, 1f, 1f};

        if(accelerometerRaw == null)
            return inclination;


        SensorManager.getRotationMatrix(R_matrix, I_matrix, accelerometerRaw, geomagnetic);

        SensorManager.remapCoordinateSystem(R_matrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);

        SensorManager.getOrientation(outR, inclination_rad);

        inclination[0] = (float) Math.toDegrees(inclination_rad[1]);
        inclination[1] = -(float) Math.toDegrees(inclination_rad[2]);


        return inclination;

    }

    /**
     * calculates a new value for the velocity of the device based on the last known linear acceleration, last known velocity and the
     * instant of time in which occurred the previous update on acceleration
     * @param timeOfLastAccelerationUpdateNanoseconds instant of time in which occurred the previous update on acceleration
     * @param currentAcceleration last known linear acceleration
     * @param currentVelocity last known velocity
     * @return new value for the velocity of the device
     */
    private float[] calculateNewVelocity(float timeOfLastAccelerationUpdateNanoseconds, float[] currentAcceleration, float[] currentVelocity)
    {
        float[] velocity = new float[3];
        if(timeOfLastAccelerationUpdateNanoseconds == -1)
        {
            for(int i = 0; i <= 2; i++)
                currentVelocity[i] = 0.0f;
        }
        else
        {
            double deltaT = (System.nanoTime() - timeOfLastAccelerationUpdateNanoseconds)/1e9;
            for(int i = 0; i <= 2; i++)
                velocity[i] = currentVelocity[i] + (float) deltaT*currentAcceleration[i]; //"integration" of acceleration
        }
        instantOfLastAccelerationUpdateNanoseconds = System.nanoTime();
        return velocity;
    }


    /**
     * Determines whether the device is in motion (which here means not sitting on a table)
     * False-positives (device being still and method says it is in motion) is more common than
     * false-negatives (device being in motion and method says it is not in motion)
     * @return true if in motion, false if not
     */
    public boolean extractInMotionRecent()
    {
        boolean ret = false;
        //if device has significant motion or motion detect sensors, return is determined by those sensors
        if(getSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null)
        {
            ret =  significantMotionRecent;
        }
        else if(getSensor(Sensor.TYPE_MOTION_DETECT) != null)
        {
            ret = motionDetectRecent;
        }
        else if(inclinationListRecent != null)
        {
            //if device doesn't have those sensors, return is determined by whether in the inclination array the maximum and minimum values differ by more
            //than a certain threshold
            ret = Math.abs(MathExtra.listMax(inclinationListRecent, 0) - MathExtra.listMin(inclinationListRecent, 0)) > MIN_INCLINATION_FOR_MOTION
                    || Math.abs(MathExtra.listMax(inclinationListRecent, 1) - MathExtra.listMin(inclinationListRecent, 1)) > MIN_INCLINATION_FOR_MOTION;
        }

        motionDetectRecent = false;
        significantMotionRecent = false;
        inclinationListRecent = new ArrayList<>();

        if(ret)
            hasBeenInMotion = true;


        return ret;
    }



    /**
     * enables the motion sensors necessary based on the AccelerationAndGravityAcquisitionMode
     */
    private void enableMotionSensors()
    {
        mSensorManager.unregisterListener(this);

        if(getSensor(Sensor.TYPE_ACCELEROMETER) != null)
            mSensorManager.registerListener(this, getSensor(Sensor.TYPE_ACCELEROMETER), sensors_delay);

        switch (AccelerationAndGravityAcquisitionMode)
        {
            case LIN_ACC:
                mSensorManager.registerListener(this, getSensor(Sensor.TYPE_LINEAR_ACCELERATION), sensors_delay);
                break;
            case GRAV_and_ACC:
                mSensorManager.registerListener(this, getSensor(Sensor.TYPE_GRAVITY), sensors_delay);
                break;
        }

        if(getSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            mSensorManager.registerListener(this, getSensor(Sensor.TYPE_SIGNIFICANT_MOTION), sensors_delay);

        }
        else if(getSensor(Sensor.TYPE_MOTION_DETECT) != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            mSensorManager.registerListener(this, getSensor(Sensor.TYPE_MOTION_DETECT), sensors_delay);
        }

    }

    /**
     * Returns and clear all the values collected since the last extract
     * @return motionValues containing values regarding accelerations, inclinations, velocities and whether device is in motion
     */
    public MotionValues extractMotionValues()
    {
        List<float[]> inclinationListTemp = inclinationList;
        inclinationList = new ArrayList<>();
        List<float[]> accelerationListTemp = accelerationList;
        accelerationList = new ArrayList<>();
        List<float[]> velocityListTemp = velocityList;
        velocityList = new ArrayList<>();


        boolean inMotion = hasBeenInMotion;
        hasBeenInMotion = false;

        List<Float> velocityModules = new ArrayList<>();
        for(float[] velocity:velocityListTemp)
        {
            velocityModules.add(MathExtra.vectorModule(velocity));
        }
        float standardDeviationVelocity = MathExtra.listStdDev(velocityModules);
        float averageVelocity = 0.0f;

        if(inMotion)
        {
            averageVelocity = MathExtra.listAvg(velocityModules);
        }
        else
        {
            //if not in motion, set velocity to 0
            averageVelocity = 0.0f;
            currentVelocity = new float[3];
        }


        List<Float> accelerationModules = new ArrayList<>();
        for(float[] acceleration:accelerationListTemp)
        {
            accelerationModules.add(MathExtra.vectorModule(acceleration));
        }
        float averageAcceleration = MathExtra.listAvg(accelerationModules);
        float standardDeviationAcceleration = MathExtra.listStdDev(accelerationModules);



        float[] averageInclination = MathExtra.listAvg(inclinationListTemp, 2);
        float[] standardDeviationInclination = MathExtra.listStdDev(inclinationListTemp, 2);

        return new MotionValues(averageAcceleration, standardDeviationAcceleration, averageVelocity, standardDeviationVelocity, averageInclination[0], standardDeviationInclination[0], averageInclination[1],standardDeviationInclination[1], inMotion);
    }

    /**
     * Update energy mode and the sensor delays accordingly
     * @param mode - EnergyMode
     */
    public void setModeAndUpdate(int mode) {

        boolean update = false;
        if (this.mode != mode && (mode == EnergyModes.MODE_HIGH_BATTERY_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_INMOTION || mode == EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION || mode == EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION)) {
            switch (mode)
            {
                case EnergyModes.MODE_HIGH_BATTERY_INMOTION:
                    sensors_delay = 200000; //.2s
                    break;
                case EnergyModes.MODE_LOW_BATTERY_INMOTION:
                    sensors_delay = 500000; //.5s
                    break;
                case EnergyModes.MODE_HIGH_BATTERY_NOT_INMOTION:
                    sensors_delay = 1000000;//1s
                    break;
                case EnergyModes.MODE_LOW_BATTERY_NOT_INMOTION:
                    sensors_delay = 2000000;//2s
                    break;
            }
            this.mode = mode;
            update = true;
        }

        if(update)
            enableMotionSensors();
    }

    public void onDestroy()
    {
        disableSensors();
    }

    private void disableSensors() {
        mSensorManager.unregisterListener(this);
    }


    //--------------------SensorEventListener--------------------------

    /**
     * updates the values of accelerations, gravities, velocities, inclinations... according to the type of sensor fired
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            float[] currentAccelerometerRaw = new float[3];//raw acceleration on x, y and z axis
            System.arraycopy(sensorEvent.values, 0, currentAccelerometerRaw, 0, 3);
            float[] currentInclination = calculateNewInclination(currentAccelerometerRaw);
            if(inclinationList != null)
                inclinationList.add(currentInclination);
            if(inclinationListRecent != null)
                inclinationListRecent.add(currentInclination);

        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY || sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER ||
                sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)
        {
            float[] newGravity = calculateNewGravity(sensorEvent, currentGravity);
            if (newGravity != null)
            {
                currentGravity = newGravity;
            }

        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION || sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER ||
                sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)
        {

            float[] newAcceleration = calculateNewAcceleration(sensorEvent, currentGravity);
            if(newAcceleration != null)
            {
                currentAcceleration = newAcceleration;
                if(accelerationList != null)
                    accelerationList.add(currentAcceleration);
                float[] newVelocity = calculateNewVelocity(instantOfLastAccelerationUpdateNanoseconds, currentAcceleration, currentVelocity);
                if(newVelocity != null)
                {
                    currentVelocity = newVelocity;
                    if(velocityList != null)
                        velocityList.add(currentVelocity);
                }

            }

        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION)
        {
            significantMotionRecent = true;
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_MOTION_DETECT)
        {
            motionDetectRecent = true;
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }




}



