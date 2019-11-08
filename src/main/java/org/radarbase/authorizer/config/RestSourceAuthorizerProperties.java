/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.config;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "rest-source-authorizer", ignoreUnknownFields = false)
public class RestSourceAuthorizerProperties {

    private CorsConfiguration cors;

    private String sourceClientsFilePath;

    private String validator;

    private ManagementPortalProperties managementPortal;

    public CorsConfiguration getCors() {
        return cors;
    }

    public void setCors(CorsConfiguration cors) {
        this.cors = cors;
    }

    public String getSourceClientsFilePath() {
        return sourceClientsFilePath;
    }

    public void setSourceClientsFilePath(String sourceClientsFilePath) {
        this.sourceClientsFilePath = sourceClientsFilePath;
    }

    public String getValidator() {
        return validator;
    }

    public void setValidator(String validator) {
        this.validator = validator;
    }

    public ManagementPortalProperties getManagementPortal() {
        return managementPortal;
    }

    public void setManagementPortal(
        ManagementPortalProperties managementPortal) {
        this.managementPortal = managementPortal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestSourceAuthorizerProperties that = (RestSourceAuthorizerProperties) o;
        return Objects.equals(cors, that.cors)
                && Objects.equals(sourceClientsFilePath, that.sourceClientsFilePath);
    }

    @Override
    public int hashCode() {

        return Objects.hash(cors, sourceClientsFilePath);
    }
}
