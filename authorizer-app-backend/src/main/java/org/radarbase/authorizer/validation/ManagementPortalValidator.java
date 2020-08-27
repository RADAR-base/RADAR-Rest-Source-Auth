//package org.radarbase.authorizer.validation;
//
//import static org.radarbase.authorizer.validation.ManagementPortalValidator.MP_VALIDATOR_PROPERTY_VALUE;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
//import org.radarbase.authorizer.service.dto.managementportal.Subject;
//import org.radarbase.authorizer.service.managementportal.ManagementPortalClient;
//import org.radarbase.authorizer.validation.exception.ValidationFailedException;
//import org.radarcns.exception.TokenException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//
//@Component
//@ConditionalOnProperty(value = "rest-source-authorizer.validator", havingValue = MP_VALIDATOR_PROPERTY_VALUE)
//public class ManagementPortalValidator implements Validator {
//
//  public static final String MP_VALIDATOR_PROPERTY_VALUE = "managementportal";
//
//  private static final Logger logger = LoggerFactory.getLogger(ManagementPortalValidator.class);
//  private final ManagementPortalClient mpClient;
//
//  @Autowired
//  public ManagementPortalValidator(ManagementPortalClient mpClient) {
//    this.mpClient = mpClient;
//  }
//
//  @Override
//  public boolean validate(RestSourceUserPropertiesDTO restSourceUser) {
//    if (restSourceUser == null) {
//      return false;
//    }
//
//    Subject subject;
//    try {
//      subject = mpClient.getSubject(restSourceUser.getUserId());
//    } catch (TokenException exc) {
//      logger.warn("Cannot get a valid token from Management Portal.", exc);
//      throw new ValidationFailedException(restSourceUser, this,
//          "Cannot get a valid token from Management Portal. " + exc.getMessage());
//    } catch (MalformedURLException exc) {
//      logger.warn("URL is mis-configured.", exc);
//      throw new ValidationFailedException(restSourceUser, this,
//          "URL is mis-configured. " + exc.getMessage());
//    } catch (IOException exc) {
//      logger.warn("An error occurred while making Validating request.", exc);
//      throw new ValidationFailedException(restSourceUser, this,
//          "An error occurred while making Validating request. " + exc.getMessage());
//    }
//    if (subject != null) {
//      return subject.getProject().getProjectId().equals(restSourceUser.getProjectId());
//    } else {
//      logger.warn("The subject with id {} was not found in the Management portal.",
//          restSourceUser.getUserId());
//      throw new ValidationFailedException(restSourceUser, this,
//          "The subject with id " + restSourceUser.getUserId()
//              + " was not found in the Management portal."
//      );
//    }
//  }
//}
