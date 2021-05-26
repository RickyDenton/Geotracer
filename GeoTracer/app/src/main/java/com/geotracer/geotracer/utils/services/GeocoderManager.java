package com.geotracer.geotracer.utils.services;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.util.Log;

//import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/*
        This class provides static methods for manipulating locations.
        To check the logs use the string "GeocoderManager".
*/
public class GeocoderManager {

    //what is the maximum distance allowed before an update between the current location and the last location registered
    private static int maxDistanceBeforeUpdating = 5; //in meters!

    //This method allows you to get the city from the location given in input
    public static String convertLocationToPlace(Location location, Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        //before making a request we check that the position is not sufficiently different from the last one registered
        if(compareWithLastRegisteredLocation(context,location)){

            try {
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(3);

                Geocoder geocoder = new Geocoder(context);
                List<Address> addresses = null;
                addresses = geocoder.getFromLocation(Double.parseDouble(df.format(location.getLatitude()).replaceFirst(",",".")),Double.parseDouble(df.format(location.getLongitude()).replaceFirst(",",".")), 1);
                if (addresses.size() > 0) {
                    String city = addresses.get(0).getLocality();
                    Log.d("GeocoderManager", "conversion successful: " + city);
                    //save as last city
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("GeocoderManager_lastCityRegistered", city);
                    editor.apply();
                    Log.d("GeocoderManager", "new location and city registered");
                    return city;
                } else {
                    Log.d("GeocoderManager", "no conversions found");
                    return "";
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("GeocoderManager", "Error during conversion from location to place:"+e.getMessage());
                //return the last city
                String lastCityRegistered = sharedPref.getString("GeocoderManager_lastCityRegistered", "");
                Log.d("GeocoderManager", "the last registered city is: "+lastCityRegistered);
                return lastCityRegistered;
            }

        }else {
           //return the last city
            String lastCityRegistered = sharedPref.getString("GeocoderManager_lastCityRegistered", "");
            Log.d("GeocoderManager", "the last registered city is: "+lastCityRegistered);
            return lastCityRegistered;
        }
    }

    private static boolean compareWithLastRegisteredLocation(Context context,Location currentLocation) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String lastLocationRegistered = sharedPref.getString("GeocoderManager_lastLocationRegistered", "empty");
        Log.d("GeocoderManager", "the last registered location is: "+lastLocationRegistered);

        String[] last_splitted = lastLocationRegistered.split(",");
        Location last = new Location(LocationManager.GPS_PROVIDER);


        if(lastLocationRegistered.compareTo("empty")!=0) {
            last.setLatitude(Double.parseDouble(last_splitted[0]));
            last.setLongitude(Double.parseDouble(last_splitted[1]));
        }

        //if is empty or the difference between the current is is more than maxDistanceFromLast meters register the new one
        if(lastLocationRegistered.compareTo("empty")==0 ||
           last.distanceTo(currentLocation)>maxDistanceBeforeUpdating) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("GeocoderManager_lastLocationRegistered", currentLocation.getLatitude() + "," + currentLocation.getLongitude());
            editor.apply();
            Log.d("GeocoderManager", "new location registered");
            return true;
        }

        Log.d("GeocoderManager", "no new location has been registered. The distance with the last is: "+last.distanceTo(currentLocation));
        return false;
    }
}
