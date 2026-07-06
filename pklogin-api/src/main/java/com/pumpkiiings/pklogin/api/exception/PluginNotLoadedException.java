package com.pumpkiiings.pklogin.api.exception;

public class PluginNotLoadedException extends RuntimeException {
    public PluginNotLoadedException(String message) {
        super(message);
    }
    public PluginNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }
}
