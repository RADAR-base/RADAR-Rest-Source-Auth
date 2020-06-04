package org.radarbase.authorizer.security;

import org.radarbase.authorizer.config.RestSourceAuthorizerProperties;
import org.radarcns.auth.authentication.TokenValidator;
import org.radarcns.auth.config.TokenVerifierPublicKeyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class CustomWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    private RestSourceAuthorizerProperties config;

    @Override
    public void configure(WebSecurity web) {
        // Overridden to exclude some url's
        web.ignoring().antMatchers("/health");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.addFilterAfter(new JwtAuthenticationFilter(getTokenValidator()),
                BasicAuthenticationFilter.class);
    }

    @Bean
    public TokenValidator getTokenValidator() {
        TokenVerifierPublicKeyConfig authVerifierConfig = new TokenVerifierPublicKeyConfig();
        authVerifierConfig.setPublicKeys(config.getAuth().getPublicKeys());
        authVerifierConfig.setPublicKeyEndpoints(config.getAuth().getPublicKeyEndpoints());
        authVerifierConfig.setResourceName(config.getAuth().getResourceName());
        return new TokenValidator(authVerifierConfig);
    }

}