package io.doorbell.android.callbacks;

public interface OnErrorCallback {
    void error(String message);
    void error(Exception exception);
}
