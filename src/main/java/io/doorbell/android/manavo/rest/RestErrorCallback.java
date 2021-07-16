package io.doorbell.android.manavo.rest;


public interface RestErrorCallback {
    void error(String message);
    void error(Exception exception);
}
