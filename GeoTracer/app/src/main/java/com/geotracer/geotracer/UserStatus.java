package com.geotracer.geotracer;

import android.app.Application;

public class UserStatus extends Application {
    private boolean contacts = false;

    public boolean getContacts() {
        return contacts;
    }

    public void setContacts(boolean contacts) {
        this.contacts = contacts;
    }



}
