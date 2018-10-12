package org.radarbase.authorizer.webapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.EXPECTATION_FAILED)
public class InvalidDeviceTypeException extends RuntimeException{

    public InvalidDeviceTypeException(String deviceType) {
        super("Unsupported device type found");
    }

    public InvalidDeviceTypeException(String deviceType, Throwable cause) {
        super("Cannot find configurations for device-type " + deviceType, cause);
    }
}
