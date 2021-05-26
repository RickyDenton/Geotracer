package com.geotracer.geotracer.utils.generics;

//// OP STATUS
//   Bean class to return a valud coupled with the status of the operation
public class RetStatus<T>{

    private final T value;
    private final OpStatus result;

    public RetStatus(T value, OpStatus error){
        this.value = value;
        this.result = error;
    }

    public T getValue(){
        if( value == null || result != OpStatus.OK)
            return null;
        return value;
    }

    public OpStatus getStatus(){
        return this.result;
    }

}
