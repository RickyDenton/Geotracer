package com.geotracer.geotracer.notifications;

import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.firebase.firestore.FirebaseFirestore;
import com.geotracer.geotracer.utils.data.Signature;
import org.jetbrains.annotations.NotNull;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;
import android.content.Context;
import java.util.Collections;
import androidx.work.Worker;
import java.util.ArrayList;
import io.paperdb.Paper;
import android.util.Log;
import java.util.List;


//// INFECTION ALARM
//   Worker for flooding the user signatures in all the registered buckets
public class InfectionAlarm extends Worker {

    private final FirebaseFirestore db;
    private static final String TAG = "InfectionAlarm";

    public InfectionAlarm(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        Paper.init(context);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public @NotNull Result doWork() {

        try {

            Log.d(TAG, "[Infection Alert] Starting collect user beacons");
            //  getting all the stored signatures
            RetStatus<List<Signature>> signatures = getAllSignatures();
            if (signatures.getStatus() != OpStatus.OK)
                return Result.failure();
            //  getting all the stored buckets
            RetStatus<List<String>> buckets = getBuckets();
            if( buckets.getStatus() != OpStatus.OK )
                return Result.retry();

            //  we send all the signatures in all the buckets
            buckets.getValue().forEach( bucket -> signatures.getValue().forEach(signature -> db.collection(bucket).add(signature)));

            //  the signatures are no more usable, we remove all the signatures
            Paper.book("signatures").destroy();
            return Result.success();

        }catch(RuntimeException e){

            e.printStackTrace();
            return Result.failure();

        }
    }

    //  get all the valid stored signatures ordered by their expire date
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.EMPTY: the operation went well but no signature is present
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<Signature>> getAllSignatures(){

        try{

            //  getting all the signatures available inside the key-value database
            List<Signature> signatures = new ArrayList<>();
            Paper.book("signatures").getAllKeys().forEach(
                    s -> signatures.add(new Signature(Paper.book("signatures").read(s))));

            //  reordering of the signatures basing on their expire time
            Collections.sort(signatures);

            //  by the reordering we can get all the valid signatures by just splitting the list
            //  from the first valid signature encountered [invalid,invalid, valid, valid, valid..]
            for( int a = 0; a<signatures.size(); a++)
                if( !signatures.get(a).isExpired()){

                    Log.d(TAG,"All the signatures collected. Number of signatures: "+
                            signatures.size() + " Effective signatures: " + (signatures.size()-a));
                    return new RetStatus<>(signatures.subList(a, signatures.size()), OpStatus.OK);

                }

            //  no valid signatures present
            Log.d(TAG,"All the signatures collected. Number of signatures: "+
                    signatures.size() + " Effective signatures: 0");
            return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(new ArrayList<>(), OpStatus.ERROR);

        }
    }

    //  returns all the saved buckets
    //  Returns:
    //      - OpStatus.OK: return all the saved buckets
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<String>> getBuckets(){

        try {

            return new RetStatus<>(Paper.book("buckets").getAllKeys(),OpStatus.OK);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);

        }
    }

}
