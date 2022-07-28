package com.thalesgroup.sensorlogging;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * represents a set of values related to the motion sensors for a period of time
 */
public class MotionValues extends RealmObject
{

    @PrimaryKey
    private long id;
    private float averageAcceleration;
    private float standardDeviationAcceleration;
    private float averageVelocity;
    private float standardDeviationVelocity;
    private float averageInclinationX;
    private float standardDeviationInclinationX;
    private float averageInclinationY;
    private float standardDeviationInclinationY;
    private boolean inMotion;

    public MotionValues(float averageAcceleration, float standardDeviationAcceleration, float averageVelocity, float standardDeviationVelocity, float averageInclinationX, float standardDeviationInclinationX, float averageInclinationY, float standardDeviationInclinationY, boolean inMotion) {
        this.averageAcceleration = averageAcceleration;
        this.standardDeviationAcceleration = standardDeviationAcceleration;
        this.averageVelocity = averageVelocity;
        this.standardDeviationVelocity = standardDeviationVelocity;
        this.averageInclinationX = averageInclinationX;
        this.standardDeviationInclinationX = standardDeviationInclinationX;
        this.averageInclinationY = averageInclinationY;
        this.standardDeviationInclinationY = standardDeviationInclinationY;
        this.inMotion = inMotion;
    }

    public MotionValues() {
    }

    @Override
    public String toString() {
        return "MotionValues{" +
                "id=" + id +
                ", averageAcceleration=" + averageAcceleration +
                ", standardDeviationAcceleration=" + standardDeviationAcceleration +
                ", averageVelocity=" + averageVelocity +
                ", standardDeviationVelocity=" + standardDeviationVelocity +
                ", averageInclinationX=" + averageInclinationX +
                ", standardDeviationInclinationX=" + standardDeviationInclinationX +
                ", averageInclinationY=" + averageInclinationY +
                ", standardDeviationInclinationY=" + standardDeviationInclinationY +
                ", inMotion=" + inMotion +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public float getAverageAcceleration() {
        return averageAcceleration;
    }

    public void setAverageAcceleration(float averageAcceleration) {
        this.averageAcceleration = averageAcceleration;
    }

    public float getStandardDeviationAcceleration() {
        return standardDeviationAcceleration;
    }

    public void setStandardDeviationAcceleration(float standardDeviationAcceleration) {
        this.standardDeviationAcceleration = standardDeviationAcceleration;
    }

    public float getAverageVelocity() {
        return averageVelocity;
    }

    public void setAverageVelocity(float averageVelocity) {
        this.averageVelocity = averageVelocity;
    }

    public float getStandardDeviationVelocity() {
        return standardDeviationVelocity;
    }

    public void setStandardDeviationVelocity(float standardDeviationVelocity) {
        this.standardDeviationVelocity = standardDeviationVelocity;
    }

    public float getAverageInclinationX() {
        return averageInclinationX;
    }

    public void setAverageInclinationX(float averageInclinationX) {
        this.averageInclinationX = averageInclinationX;
    }

    public float getStandardDeviationInclinationX() {
        return standardDeviationInclinationX;
    }

    public void setStandardDeviationInclinationX(float standardDeviationInclinationX) {
        this.standardDeviationInclinationX = standardDeviationInclinationX;
    }

    public float getAverageInclinationY() {
        return averageInclinationY;
    }

    public void setAverageInclinationY(float averageInclinationY) {
        this.averageInclinationY = averageInclinationY;
    }

    public float getStandardDeviationInclinationY() {
        return standardDeviationInclinationY;
    }

    public void setStandardDeviationInclinationY(float standardDeviationInclinationY) {
        this.standardDeviationInclinationY = standardDeviationInclinationY;
    }

    public boolean isInMotion() {
        return inMotion;
    }

    public void setInMotion(boolean inMotion) {
        this.inMotion = inMotion;
    }
}
