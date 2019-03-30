package com.example.extractreader;

public class SpaceVehicle {
    int SvId;
    float fastCorrection;
    int udrei;
    int weekNum;
    float weekSecs;

    public static SpaceVehicle[] setSpaceVehicleArray(int size){
        SpaceVehicle[] mSpaceVehicle = new SpaceVehicle[size];
        for (int i = 0; i < size; i++){
            mSpaceVehicle[i] = new SpaceVehicle();
        }
        return mSpaceVehicle;
    }
}
