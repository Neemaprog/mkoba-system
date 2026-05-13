package com.mkoba.mkoba_system.services;

import com.mkoba.mkoba_system.config.AzamPayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

@Service
public class AzamPayService {
    
    @Autowired
    private AzamPayConfig azamPayConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Initiate payment with AzamPay
     */
    public Map<String, Object> initiatePayment(String amount, String phoneNumber, String reference, String description) {
        try {
            System.out.println("🔧 Initiating AzamPay payment...");
            System.out.println("📊 Amount: " + amount);
            System.out.println("📱 Phone: " + phoneNumber);
            System.out.println("🔗 Reference: " + reference);
            System.out.println("📝 Description: " + description);
            System.out.println("🌐 API URL: " + azamPayConfig.getApiUrl());
            System.out.println("🔑 Client ID: " + azamPayConfig.getClientId());
            System.out.println("🎫 Token: " + azamPayConfig.getToken().substring(0, 20) + "...");
            
            // REAL AZAMPAY API CODE
            // Toggle between REAL and MOCK by changing this boolean
            boolean USE_MOCK = true;  // Set to true for mock, false for real API
            
            if (USE_MOCK) {
                System.out.println("🔧 Using MOCK AzamPay API for testing...");
                return createMockResponse(amount, phoneNumber, reference, description);
            }
            
            System.out.println("🌐 Using REAL AzamPay API...");
            
            // Create payment request for MNO Checkout
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("accountNumber", phoneNumber);  // Mobile number
            paymentRequest.put("amount", amount);
            paymentRequest.put("currency", "TZS");
            paymentRequest.put("externalId", reference);  // Reference ID
            
            // Detect mobile provider based on phone number
            String provider = detectMobileProvider(phoneNumber);
            paymentRequest.put("provider", provider);
            
            paymentRequest.put("additionalProperties", Map.of(
                "callbackUrl", azamPayConfig.getCallbackUrl(),
                "redirectUrl", azamPayConfig.getRedirectUrl(),
                "description", description
            ));
            
            System.out.println("📤 Payment Request: " + paymentRequest);
            System.out.println("📱 Detected Provider: " + provider);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-ID", azamPayConfig.getClientId());
            headers.set("Authorization", "Bearer " + azamPayConfig.getToken());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, headers);
            
            // Make API call
            String url = azamPayConfig.getApiUrl() + "/mno/checkout";  // Updated endpoint
            System.out.println("🚀 Making REAL API call to: " + url);
            System.out.println("📤 Request Headers:");
            System.out.println("   Content-Type: " + headers.getContentType());
            System.out.println("   X-Client-ID: " + headers.get("X-Client-ID"));
            System.out.println("   Authorization: Bearer " + azamPayConfig.getToken().substring(0, 20) + "...");
            System.out.println("📤 Request Body: " + paymentRequest);
            
            // Add retry logic for connection issues
            ResponseEntity<Map> response = null;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (retryCount < maxRetries && response == null) {
                try {
                    response = restTemplate.postForEntity(url, entity, Map.class);
                } catch (Exception e) {
                    retryCount++;
                    System.err.println("❌ Retry " + retryCount + "/" + maxRetries + ": " + e.getMessage());
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000); // Wait 1 second before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (response == null) {
                throw new RuntimeException("Failed to connect to AzamPay after " + maxRetries + " attempts");
            }
            
            System.out.println("✅ AzamPay Response Status: " + response.getStatusCode());
            System.out.println("✅ AzamPay Response Headers: " + response.getHeaders());
            System.out.println("✅ AzamPay Response Body: " + response.getBody());
            
            // Handle empty response
            if (response.getBody() == null) {
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("status", "PENDING");
                fallbackResponse.put("message", "Payment initiated but waiting for confirmation");
                fallbackResponse.put("transactionId", "TXN_" + System.currentTimeMillis());
                fallbackResponse.put("paymentUrl", azamPayConfig.getRedirectUrl());
                return fallbackResponse;
            }
            
            return response.getBody();
            
            // MOCK RESPONSE CODE (commented out)
            /*
            System.out.println("🔧 Using mock AzamPay response for testing...");
            
            Map<String, Object> mockResponse = new HashMap<>();
            mockResponse.put("transactionId", "MOCK_TXN_" + System.currentTimeMillis());
            mockResponse.put("status", "SUCCESS");
            mockResponse.put("message", "Mock payment initiated successfully for testing");
            mockResponse.put("paymentUrl", azamPayConfig.getRedirectUrl() + "?mock=true&transactionId=" + mockResponse.get("transactionId"));
            mockResponse.put("amount", amount);
            mockResponse.put("phoneNumber", phoneNumber);
            mockResponse.put("currency", "TZS");
            mockResponse.put("reference", reference);
            mockResponse.put("description", description);
            mockResponse.put("callbackUrl", azamPayConfig.getCallbackUrl());
            mockResponse.put("redirectUrl", azamPayConfig.getRedirectUrl());
            
            System.out.println("✅ Mock AzamPay Response: " + mockResponse);
            return mockResponse;
            */
            
        } catch (Exception e) {
            System.err.println("❌ Error initiating AzamPay payment: " + e.getMessage());
            System.err.println("❌ Full error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.err.println("❌ Error details:");
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                System.err.println("   404 Error: Endpoint not found - Check API URL");
            }
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                System.err.println("   401 Error: Authentication failed - Check credentials");
            }
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                System.err.println("   403 Error: Forbidden - Check permissions");
            }
            if (e.getMessage() != null && e.getMessage().contains("500")) {
                System.err.println("   500 Error: Server error - Try again later");
            }
            e.printStackTrace();
            
            // Return fallback response instead of throwing exception
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Payment initiation failed: " + e.getMessage());
            errorResponse.put("transactionId", "ERROR_" + System.currentTimeMillis());
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorCode", e.getClass().getSimpleName());
            return errorResponse;
        }
    }
    
    /**
     * Create mock response for testing
     */
    private Map<String, Object> createMockResponse(String amount, String phoneNumber, String reference, String description) {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "PENDING");
        mockResponse.put("message", "Mock payment initiated successfully for testing");
        mockResponse.put("transactionId", "MOCK_TXN_" + System.currentTimeMillis());
        mockResponse.put("paymentUrl", azamPayConfig.getRedirectUrl() + "?mock=true&transactionId=" + mockResponse.get("transactionId"));
        mockResponse.put("amount", amount);
        mockResponse.put("phoneNumber", phoneNumber);
        mockResponse.put("currency", "TZS");
        mockResponse.put("reference", reference);
        mockResponse.put("description", description);
        mockResponse.put("callbackUrl", azamPayConfig.getCallbackUrl());
        mockResponse.put("redirectUrl", azamPayConfig.getRedirectUrl());
        mockResponse.put("sandbox", true);
        
        System.out.println("✅ Mock AzamPay Response: " + mockResponse);
        return mockResponse;
    }
    
    /**
     * Detect mobile provider based on phone number prefix
     */
    private String detectMobileProvider(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "Mpesa"; // Default
        }
        
        // Extract prefix after country code
        String prefix = phoneNumber.startsWith("255") ? phoneNumber.substring(3, 6) : phoneNumber.substring(0, 6);
        
        // Tanzania mobile operator prefixes
        switch (prefix) {
            case "754":
            case "755":
            case "756":
            case "757":
            case "758":
            case "759":
            case "760":
            case "761":
            case "762":
            case "763":
            case "764":
            case "765":
            case "766":
            case "767":
            case "768":
            case "769":
                return "Mpesa";
            case "713":
            case "714":
            case "715":
            case "716":
            case "717":
            case "718":
            case "719":
                return "Tigo";
            case "687":
            case "688":
            case "689":
                return "Airtel";
            case "655":
            case "656":
            case "657":
            case "658":
            case "659":
                return "Halopesa";
            case "671":
            case "672":
            case "673":
            case "674":
            case "675":
            case "676":
            case "677":
            case "678":
            case "679":
                return "Azampesa";
            default:
                return "Mpesa"; // Default fallback
        }
    }
    
    /**
     * Check payment status
     */
    public Map<String, Object> checkPaymentStatus(String transactionId) {
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-ID", azamPayConfig.getClientId());
            headers.set("Authorization", "Bearer " + azamPayConfig.getToken());
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make API call
            String url = azamPayConfig.getApiUrl() + "/payments/status/" + transactionId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            System.err.println("❌ Error checking payment status: " + e.getMessage());
            throw new RuntimeException("Failed to check payment status", e);
        }
    }
    
    /**
     * Validate AzamPay webhook signature
     */
    public boolean validateWebhookSignature(String payload, String signature) {
        try {
            // TODO: Implement webhook signature validation
            // This would involve using the client secret to verify the signature
            return true; // For now, accept all webhooks in sandbox
        } catch (Exception e) {
            System.err.println("❌ Error validating webhook signature: " + e.getMessage());
            return false;
        }
    }
}
