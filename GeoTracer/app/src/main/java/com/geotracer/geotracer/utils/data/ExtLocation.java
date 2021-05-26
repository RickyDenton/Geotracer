package com.geotracer.geotracer.utils.data;

import android.location.Location;
import android.util.Log;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Date;

import static android.content.ContentValues.TAG;

//// EXT LOCATION
//   Bean class to store an heatmap point
@SuppressWarnings("unused")
public class ExtLocation extends BaseLocation{

    private boolean infected;
    private int criticity;
    private final String geoHash;

    public ExtLocation(GeoPoint location){

        super(location);
        this.infected = false;
        this.criticity = 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, 1);
        this.expire = calendar.getTime();
        this.geoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(location.getLatitude(), location.getLongitude()));

    }

    public ExtLocation(GeoPoint location, Date expire, Boolean infected, Long criticity, String geoHash){

        this.location = location;
        this.expire = expire;
        this.infected = infected;
        this.criticity = Math.toIntExact(criticity);
        this.geoHash = geoHash;

    }

    public ExtLocation( String jsonObject ){
        super();
        Gson gson = new Gson();
        ExtLocation converter = gson.fromJson(jsonObject,ExtLocation.class);
        this.location = converter.location;
        this.expire = converter.expire;
        this.infected = converter.infected;
        this.criticity = converter.criticity;
        this.geoHash = converter.geoHash;
    }

    public ExtLocation setInfected(){
        infected = true;
        return this;
    }

    public void setCriticity(int criticity){
        this.criticity = criticity;
    }

    public OpStatus incrementCriticity(BaseLocation location, boolean present){

        if(expire.before(new Date()) || pointsDistance(location) > 100 ) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            expire = calendar.getTime();
            return OpStatus.UPDATE_LOCATION;
        }

        if(!present)
            this.criticity++;

        return OpStatus.OK;
    }

    public float pointsDistance(BaseLocation location){

        float[] results = new float[1];
        Location.distanceBetween(this.location.getLatitude(),this.location.getLongitude(),
                location.getLocation().getLatitude(),location.getLocation().getLongitude(),
                results);
        return results[0];
    }

    public boolean getInfected(){
        return infected;
    }

    public String getGeoHash(){
        return geoHash;
    }

    public int getCriticity(){
        return criticity;
    }
}
