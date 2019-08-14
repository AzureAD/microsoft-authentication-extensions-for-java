package com.microsoft.azure;

public class CrossPlatLockNotObtainedException extends Exception {

    public CrossPlatLockNotObtainedException(String message) {
        super(message);
    }
}
