package com.geotracer.geotracer.utils.data;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;


//// BASE LOCATION
//   Bean class to store the user position
@SuppressWarnings("unused")
public class BaseLocation implements Comparable<BaseLocation>{

    protected GeoPoint location;
    protected Date expire;

    protected BaseLocation(){}

    public BaseLocation(GeoPoint location){

        this.location = location;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        expire = calendar.getTime();
    }

    BaseLocation( String jsonObject ){

        Gson gson = new Gson();
        BaseLocation converter = gson.fromJson(jsonObject,BaseLocation.class);
        this.location = converter.location;
        this.expire = converter.expire;

    }

    public GeoPoint getLocation(){
        return location;
    }

    public Date getExpire(){ return expire; }

    public boolean isExpired(){
        return new Date().after(expire);
    }

    @Override
    public @NotNull String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public int compareTo(BaseLocation o) {

        if( expire.after(o.expire)) return 1;
        if( expire.before(o.expire)) return -1;
        return 0;

    }
}
