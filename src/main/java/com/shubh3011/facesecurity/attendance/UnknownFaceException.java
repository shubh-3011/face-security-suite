package com.shubh3011.facesecurity.attendance;

/** Thrown when a captured face does not correspond to any enrolled identity. */
public class UnknownFaceException extends Exception {

    public UnknownFaceException(String message) {
        super(message);
    }
}
