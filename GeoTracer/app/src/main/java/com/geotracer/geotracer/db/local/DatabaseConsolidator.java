package com.geotracer.geotracer.db.local;

import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.data.Signature;
import org.jetbrains.annotations.NotNull;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;
import android.content.Context;
import androidx.work.Worker;
import io.paperdb.Paper;
import android.util.Log;
import java.util.List;


//   DATABASE CONSOLIDATOR
//   Class to manage the consolidation of server. Periodically it control the local key-value database
//   and removes the expired information[ PERIOD: 1Hour CONSTRAINTS: idleDevice ]
//   The aim of the class is to reduce the weight of the stored data by periodically deleting
//   all the no more usable data

public class DatabaseConsolidator extends Worker {

    private static final String TAG = "DatabaseConsolidator";

    public DatabaseConsolidator(@NonNull Context context, @NonNull WorkerParameters params) {

        super(context, params);
        Paper.init(context);      //  initialization of Paper keystore

    }

    @Override
    public @NotNull Result doWork() {

        if( dropExpiredBeacons() == OpStatus.OK &&
                dropExpiredSignatures() == OpStatus.OK &&
                    dropExpiredPositions() == OpStatus.OK )
            return Result.success();
        return Result.failure();

    }

    //  MANAGEMENT FUNCTIONS

    //  drops the expired beacons collected from the other users inside the key-value database
    private OpStatus dropExpiredBeacons(){

        try {

            // getting all the stored beacons
            List<String> keys = Paper.book("beacons").getAllKeys();
            int before = keys.size();

            //  for each beacon we verify its validity and if it is expired we remove it
            keys.forEach(key -> {
                if ( new ExtSignature(Paper.book("beacons").read(key)).isExpired()){
                    Paper.book("beacons").delete(key);
                }
            });

            Log.d( TAG, "Consolidation of stored beacons completed: " + before + " -> " +
                    Paper.book("beacons").getAllKeys().size());
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  drops the expired user signatures stored inside the key-value database
    private OpStatus dropExpiredSignatures(){

        try {

            // getting all the stored signatures
            List<String> keys = Paper.book("signatures").getAllKeys();
            int before = keys.size();

            //  for each signature we verify its validity and if it is expired we remove it
            keys.forEach(key -> {
                if ( new Signature( Paper.book("signatures").read(key)).isExpired()) {
                    Paper.book("signatures").delete(key);
                }
            });

            Log.d( TAG,  "Consolidation of stored signatures completed: " + before + " -> "+
                    Paper.book("signatures").getAllKeys().size());
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  drops the expired user positions stored inside the key-value database
    private OpStatus dropExpiredPositions(){

        try {

            // getting all the stored positions
            List<String> keys = Paper.book("positions").getAllKeys();
            int before = keys.size();

            //  for each position we verify its validity and if it is expired we remove it
            keys.forEach(key -> {
                if (new BaseLocation( Paper.book("positions").read(key)).isExpired()) {
                    Paper.book("positions").delete(key);
                }
            });

            Log.d( TAG,  "Consolidation of stored user positions completed: " + before + " -> "+
                    Paper.book("positions").getAllKeys().size());

            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }
}
