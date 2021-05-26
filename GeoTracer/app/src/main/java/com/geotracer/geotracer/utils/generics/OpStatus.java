package com.geotracer.geotracer.utils.generics;


//// OP STATUS
//   Enumerator used for return the logic result of a function

public enum OpStatus {

    OK,
    PRESENT,
    NOT_PRESENT,
    ERROR,
    UPDATE_LOCATION,
    COLLECTED,
    UPDATED,
    ILLEGAL_ARGUMENT,
    INFECTED,
    NOT_INFECTED,
    EMPTY;

    @Override
    public String toString() throws IllegalArgumentException{

        switch(this){

            case OK:                 return "OK";
            case PRESENT:            return "BEACON_PRESENT";
            case NOT_PRESENT:        return "BEACON_NOT_PRESENT";
            case ERROR:              return "ERROR";
            case UPDATE_LOCATION:    return "UPDATE_LOCATION";
            case COLLECTED:          return "COLLECTED";
            case UPDATED:            return "UPDATED";
            case ILLEGAL_ARGUMENT:   return "ILLEGAL_ARGUMENT";
            case INFECTED:           return "INFECTED";
            case NOT_INFECTED:       return "NOT_INFECTED";
            case EMPTY:              return "EMPTY";
            default: throw new IllegalArgumentException();

        }
    }

}
