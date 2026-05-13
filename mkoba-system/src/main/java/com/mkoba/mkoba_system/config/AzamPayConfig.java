package com.mkoba.mkoba_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzamPayConfig {
    
    @Value("${azampay.client.id}")
    private String clientId;
    
    @Value("${azampay.token}")
    private String token;
    
    @Value("${azampay.client.secret}")
    private String clientSecret;
    
    @Value("${azampay.api.url}")
    private String apiUrl;
    
    @Value("${azampay.callback.url}")
    private String callbackUrl;
    
    @Value("${azampay.redirect.url}")
    private String redirectUrl;
    
    // Getters
    public String getClientId() {
        return clientId;
    }
    
    public String getToken() {
        return token;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
}
