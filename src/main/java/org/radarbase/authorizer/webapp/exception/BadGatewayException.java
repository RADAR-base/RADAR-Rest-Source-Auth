package org.radarbase.authorizer.webapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class BadGatewayException extends RuntimeException {

    public BadGatewayException() {
        super("Something went wrong in communication with other services");
    }

    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException(Throwable cause) {
        super("Something went wrong in communication with other services", cause);
    }
}
