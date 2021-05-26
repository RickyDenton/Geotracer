package com.geotracer.geotracer.db.local;

import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.data.Signature;
import java.util.Collections;
import java.util.ArrayList;
import android.util.Log;
import io.paperdb.Book;
import java.util.List;


////// SIGNATURES
//   A signature is a beacon sent by the user to other smartphones in order to generate
//   the contact tracing mechanism
//   Data Format:    SIGNATURE: { signature, expire }

@SuppressWarnings("unused")
public class SignatureUtility {

    private final Book signatures;
    private static final String TAG = "KeyValueManagement/SignatureUtility";

    //  prevents the class to be instantiated outside the package
    SignatureUtility(Book signatures){
        this.signatures = signatures;
    }

    //  insert a new signature inside the key-value database
    //  Returns:
    //      - OpStatus.OK: signature correctly inserted
    //      - OpStatus.PRESENT: signature already present, not inserted
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertSignature(Signature signature){

        if( signature == null )
            return OpStatus.ILLEGAL_ARGUMENT;

        try {

            //  verification of the signature presence
            if (signatures.contains(signature.getSignature()))
                return OpStatus.PRESENT;

            signatures.write(signature.getSignature(), signature.toString());
            Log.d( TAG,"New signature added to the key-value database: " + signature.getSignature());
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

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
            this.signatures.getAllKeys().forEach(
                    s -> signatures.add(new Signature(this.signatures.read(s))));

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

    //  clean all the signatures from the key-value database. Used after a signature spread
    //  Returns:
    //      - OpStatus.OK: collection cleaned
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus removeAllSignatures(){

        try{

            //  remove all the signatures by destroying their collection
            signatures.destroy();
            Log.d(TAG,"All the signatures removed from the key-value database");
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }
}
