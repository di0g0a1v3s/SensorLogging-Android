package com.thalesgroup.sensorlogging;

import java.util.List;

/**
  Created by thales on 26/07/2018.
 */

public abstract class MathExtra {



    public static float[] listAvg(List<float[]> list, int arraySize)
    {
        int listSize = 0;
        float[] avg = new float[arraySize];

        for(float[] aux:list)
        {
            for(int i = 0; i <= arraySize-1; i++)
            {
                avg[i] += aux[i];
            }
            listSize++;
        }

        for(int i = 0; i <= arraySize-1; i++)
        {
            avg[i] = avg[i]/listSize;
        }
        return avg;

    }

    public static float listMax(List<float[]> list, int position)
    {
        float currentMax = -Float.MAX_VALUE;
        for(float[] aux:list)
        {
            if(aux[position]>currentMax)
                currentMax = aux[position];
        }
        return currentMax;

    }

    public static float listMin(List<float[]> list, int position)
    {
        float currentMin = Float.MAX_VALUE;
        for(float[] aux:list)
        {
            if(aux[position]<currentMin)
                currentMin = aux[position];
        }
        return currentMin;

    }

    public static float listAvg(List<Float> list)
    {
        int listSize = 0;
        float avg = 0;

        for(float aux:list)
        {
            avg += aux;
            listSize++;
        }

        avg = avg/listSize;
        return avg;
    }

    public static float vectorModule(float[] vector)
    {
        return (float) Math.sqrt(Math.pow(vector[0],2)+Math.pow(vector[1],2)+Math.pow(vector[2],2));
    }


    public static float[] listStdDev(List<float[]> list, int arraySize) {

        int listSize = 0;
        float[] stdDev = new float[arraySize];

        float[] listAvg = listAvg(list,arraySize);
        for(float[] aux:list) {

            for (int i = 0; i <= arraySize - 1; i++) {

                stdDev[i] += Math.pow(aux[i] - listAvg[i], 2);
            }
            listSize++;
        }

        for(int i = 0; i <= arraySize-1; i++)
        {
            stdDev[i] = (float) Math.sqrt(stdDev[i]/listSize);
        }

        return stdDev;
    }

    public static float listStdDev(List<Float> list) {

        int listSize = 0;
        float stdDev = 0;

        float listAvg = listAvg(list);
        for(float aux:list) {

            stdDev += Math.pow(aux - listAvg, 2);

            listSize++;
        }

        stdDev = (float) Math.sqrt(stdDev/listSize);


        return stdDev;
    }
}
