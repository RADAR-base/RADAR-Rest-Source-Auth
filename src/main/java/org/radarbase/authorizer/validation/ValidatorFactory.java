package org.radarbase.authorizer.validation;

import java.util.Arrays;
import java.util.Locale;
import org.radarbase.authorizer.validation.exception.ValidatorNotFoundException;

public class ValidatorFactory {

  public static Validator getValidator(String type) throws ValidatorNotFoundException {
    if (type == null) {
      return null;
    }
    Validator validator;

    switch (ValidatorType.valueOf(type.toUpperCase(Locale.getDefault()))) {
      case MANAGEMENTPORTAL:
        validator = new ManagementPortalValidator();
        break;
      default:
        throw new ValidatorNotFoundException(
            "No validator Found with type: " + type + ". Available options are : " + Arrays
                .toString(ValidatorType.values()));
    }
    return validator;
  }

  public enum ValidatorType {
    MANAGEMENTPORTAL
  }
}
