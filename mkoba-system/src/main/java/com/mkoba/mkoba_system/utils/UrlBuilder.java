package com.mkoba.mkoba_system.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UrlBuilder {

    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    public String getBaseUrl() {
        // If BASE_URL is set (like on render), use it
        if (!"http://localhost:8080".equals(baseUrl)) {
            return baseUrl;
        }

        // Otherwise, detect from current request
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();

            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://").append(serverName);

            if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
                url.append(":").append(serverPort);
            }

            return url.toString();
        }
        return baseUrl; // fallback
    }

    public String buildCallbackUrl() {
        return getBaseUrl() + "/api/payments/callback";
    }

    public String buildRedirectUrl() {
        return getBaseUrl() + "/payments/success";
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}