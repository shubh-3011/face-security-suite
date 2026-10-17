package com.shubh3011.facesecurity.face;

/** Thrown when an image cannot be read or is not a supported format. */
public class ImageLoadException extends Exception {

    public ImageLoadException(String message) {
        super(message);
    }

    public ImageLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
