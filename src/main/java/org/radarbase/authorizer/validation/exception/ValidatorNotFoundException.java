package org.radarbase.authorizer.validation.exception;

public class ValidatorNotFoundException extends RuntimeException {

  public ValidatorNotFoundException(String message) {
    super(message);
  }

  public ValidatorNotFoundException(String message, Throwable t) {
    super(message, t);
  }
}
