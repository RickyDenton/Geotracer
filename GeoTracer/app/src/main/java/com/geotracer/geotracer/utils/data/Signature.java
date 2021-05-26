package com.geotracer.geotracer.utils.data;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;

//// SIGNATURES
//   Bean class to store the user's beacons

public class Signature implements Comparable<Signature> {

    protected String signature;
    protected Date expire;

    public Signature(){}
    public Signature(String data){
        if( data.contains("signature")) {
            Gson gson = new Gson();
            Signature converter = gson.fromJson(data, Signature.class);
            this.signature = converter.signature;
            this.expire = converter.expire;
        }else{
            this.signature = data;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, 14);
            expire = calendar.getTime();
        }
    }

    public String getSignature() {
        return signature;
    }

    public Date getExpire(){
        return expire;
    }

    public boolean isExpired() {
        return new Date().after(expire);
    }

    @Override
    public @NotNull String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public int compareTo(Signature o) {
        if (expire.after(o.expire)) return 1;
        if (expire.before(o.expire)) return -1;
        return 0;
    }
}
