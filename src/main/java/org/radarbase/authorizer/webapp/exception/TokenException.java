package org.radarbase.authorizer.webapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class TokenException extends RuntimeException{

    public TokenException() {
        super("Unable to get a valid access token");
    }

    public TokenException(String message) {
        super(message);
    }

    public TokenException(Throwable cause) {
        super("Unable to get a valid access token" , cause);
    }
}
