package com.geotracer.geotracer.utils.data;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

//// SIGNATURES
//   Bean class to store the external user's beacons
public class ExtSignature extends Signature{

    private final double distance;

    public ExtSignature(String beacon, double distance) {

        super(beacon);
        this.distance = distance;

    }

    public ExtSignature(String jsonObject){

        super();
        Gson gson = new Gson();
        ExtSignature converted = gson.fromJson(jsonObject, ExtSignature.class);
        this.signature = converted.signature;
        this.expire = converted.expire;
        this.distance = converted.distance;

    }

    public boolean toBeUpDated( double distance ){
        return this.distance> distance;
    }

    public double getDistance(){
        return distance;
    }

    @Override
    public @NotNull String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
