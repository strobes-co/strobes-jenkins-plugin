package com.wesecureapp.plugins.strobes;

public class ValidationException extends Exception {
    public ValidationException(Exception e) {
        super(e);
    }

    public ValidationException(String message) {
        super(new Exception(message));
    }
}
