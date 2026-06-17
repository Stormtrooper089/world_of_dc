package org.dcoffice.cachar.service.propertytax;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class UpyogClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${upyog.base-url}")
    private String baseUrl;
    @Value("${upyog.tenant-id}")
    private String tenantId;
    @Value("${upyog.client-id}")
    private String clientId;
    @Value("${upyog.client-secret}")
    private String clientSecret;
    @Value("${upyog.api-key}")
    private String apiKey;
    @Value("${upyog.endpoints.property-search}")
    private String propertySearchPath;
    @Value("${upyog.endpoints.demand-search}")
    private String demandSearchPath;
    @Value("${upyog.endpoints.payment-initiate}")
    private String paymentInitiatePath;
    @Value("${upyog.endpoints.receipt-search}")
    private String receiptSearchPath;
    @Value("${upyog.endpoints.service-request}")
    private String serviceRequestPath;
    @Value("${upyog.endpoints.dashboard}")
    private String dashboardPath;
    @Value("${upyog.endpoints.defaulters}")
    private String defaultersPath;

    public Map<String, Object> searchPropertyByHolding(String holdingNumber, String citizenId) {
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        body.put("citizenId", citizenId);
        return post(propertySearchPath, body);
    }

    public Map<String, Object> searchDemand(String holdingNumber) {
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        return post(demandSearchPath, body);
    }

    public Map<String, Object> initiatePayment(String holdingNumber, String citizenId, String paymentMode) {
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        body.put("citizenId", citizenId);
        body.put("paymentMode", paymentMode);
        return post(paymentInitiatePath, body);
    }

    public Map<String, Object> verifyReceipt(String receiptNumber) {
        Map<String, Object> body = requestInfo();
        body.put("receiptNumber", receiptNumber);
        return post(receiptSearchPath, body);
    }

    public Map<String, Object> createServiceRequest(String citizenId, String holdingNumber, String requestType, String remarks) {
        Map<String, Object> body = requestInfo();
        body.put("citizenId", citizenId);
        body.put("holdingNumber", holdingNumber);
        body.put("requestType", requestType);
        body.put("remarks", remarks);
        return post(serviceRequestPath, body);
    }

    public Map<String, Object> dashboard() {
        return post(dashboardPath, requestInfo());
    }

    public Map<String, Object> defaulters() {
        return post(defaultersPath, requestInfo());
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        String url = normalizeUrl(path);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers()), Map.class);
        return response.getBody() == null ? new HashMap<>() : response.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);
        headers.set("X-Client-Id", clientId);
        headers.set("X-Client-Secret", clientSecret);
        headers.set("X-Tenant-Id", tenantId);
        return headers;
    }

    private Map<String, Object> requestInfo() {
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("tenantId", tenantId);
        requestInfo.put("clientId", clientId);
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("RequestInfo", requestInfo);
        return wrapper;
    }

    private String normalizeUrl(String path) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String suffix = path.startsWith("/") ? path : "/" + path;
        return root + suffix;
    }
}
