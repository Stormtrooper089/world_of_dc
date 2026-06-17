package org.dcoffice.cachar.service.propertytax;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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
        if (isPlaceholderMode()) {
            return dummyProperty(holdingNumber);
        }
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        body.put("citizenId", citizenId);
        return post(propertySearchPath, body);
    }

    public Map<String, Object> searchDemand(String holdingNumber) {
        if (isPlaceholderMode()) {
            return dummyDemand(holdingNumber);
        }
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        return post(demandSearchPath, body);
    }

    public Map<String, Object> initiatePayment(String holdingNumber, String citizenId, String paymentMode) {
        if (isPlaceholderMode()) {
            return dummyPayment(holdingNumber, paymentMode);
        }
        Map<String, Object> body = requestInfo();
        body.put("holdingNumber", holdingNumber);
        body.put("citizenId", citizenId);
        body.put("paymentMode", paymentMode);
        return post(paymentInitiatePath, body);
    }

    public Map<String, Object> verifyReceipt(String receiptNumber) {
        if (isPlaceholderMode()) {
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("receiptNumber", receiptNumber);
            receipt.put("holdingNumber", "SMC-HLD-1001");
            receipt.put("ownerName", "UPYOG Sandbox Citizen");
            receipt.put("financialYear", "2026-27");
            receipt.put("amountPaid", 2900);
            receipt.put("paymentMode", "UPYOG_SANDBOX");
            receipt.put("transactionReference", "UPYOG-SBX-VERIFIED");
            receipt.put("status", "VERIFIED");
            return receipt;
        }
        Map<String, Object> body = requestInfo();
        body.put("receiptNumber", receiptNumber);
        return post(receiptSearchPath, body);
    }

    public Map<String, Object> createServiceRequest(String citizenId, String holdingNumber, String requestType, String remarks) {
        if (isPlaceholderMode()) {
            Map<String, Object> response = new HashMap<>();
            response.put("requestId", "UPYOG-SBX-REQ-" + System.currentTimeMillis());
            response.put("status", "SUBMITTED");
            return response;
        }
        Map<String, Object> body = requestInfo();
        body.put("citizenId", citizenId);
        body.put("holdingNumber", holdingNumber);
        body.put("requestType", requestType);
        body.put("remarks", remarks);
        return post(serviceRequestPath, body);
    }

    public Map<String, Object> dashboard() {
        if (isPlaceholderMode()) {
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalProperties", 3);
            dashboard.put("paidProperties", 0);
            dashboard.put("dueProperties", 3);
            dashboard.put("totalDemand", 24950);
            dashboard.put("totalCollected", 0);
            dashboard.put("totalOutstanding", 24950);
            dashboard.put("wardWise", List.of(
                    ward(8, "Ward 8 - Itkhola", 1, 2900),
                    ward(12, "Ward 12 - Rangirkhari", 1, 6600),
                    ward(15, "Ward 15 - Tarapur", 1, 15450)
            ));
            return dashboard;
        }
        return post(dashboardPath, requestInfo());
    }

    public Map<String, Object> defaulters() {
        if (isPlaceholderMode()) {
            Map<String, Object> response = new HashMap<>();
            response.put("defaulters", List.of(
                    defaulter("SMC-HLD-1003", "Parijat Hotel", 15, "Ward 15 - Tarapur", "Tarapur", 15450),
                    defaulter("SMC-HLD-1002", "Rina Das", 12, "Ward 12 - Rangirkhari", "Rangirkhari", 6600),
                    defaulter("SMC-HLD-1001", "UPYOG Sandbox Citizen", 8, "Ward 8 - Itkhola", "Itkhola", 2900)
            ));
            return response;
        }
        return post(defaultersPath, requestInfo());
    }

    private boolean isPlaceholderMode() {
        return baseUrl == null
                || baseUrl.contains("upyog-placeholder")
                || clientId == null
                || clientId.startsWith("dummy-")
                || apiKey == null
                || apiKey.startsWith("dummy-");
    }

    private Map<String, Object> dummyProperty(String holdingNumber) {
        Map<String, Object> property = new HashMap<>();
        property.put("holdingNumber", holdingNumber);
        property.put("propertyId", "UPYOG-SBX-" + holdingNumber);
        property.put("assessmentNumber", "UPYOG-ASMT-2026-" + holdingNumber.replaceAll("\\D", ""));
        property.put("ownerName", "UPYOG Sandbox Citizen");
        property.put("mobileNumber", "9090909090");
        property.put("wardNumber", 8);
        property.put("wardName", "Ward 8 - Itkhola");
        property.put("locality", "Itkhola");
        property.put("address", "Itkhola, Silchar, Cachar");
        property.put("propertyType", "Residential");
        property.put("usageType", "Self Occupied");
        return property;
    }

    private Map<String, Object> dummyDemand(String holdingNumber) {
        Map<String, Object> demand = new HashMap<>();
        demand.put("holdingNumber", holdingNumber);
        demand.put("financialYear", "2026-27");
        demand.put("annualTax", 2450);
        demand.put("arrears", 500);
        demand.put("penalty", 50);
        demand.put("rebate", 100);
        demand.put("amountDue", 2900);
        return demand;
    }

    private Map<String, Object> dummyPayment(String holdingNumber, String paymentMode) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("receiptNumber", "UPYOG-SBX-RCPT-" + System.currentTimeMillis());
        payment.put("holdingNumber", holdingNumber);
        payment.put("amountPaid", 2900);
        payment.put("paymentMode", paymentMode == null ? "UPYOG_SANDBOX" : paymentMode);
        payment.put("transactionReference", "UPYOG-SBX-TXN-" + System.currentTimeMillis());
        payment.put("status", "PAID");
        return payment;
    }

    private Map<String, Object> ward(int wardNumber, String wardName, int properties, int outstanding) {
        Map<String, Object> row = new HashMap<>();
        row.put("wardNumber", wardNumber);
        row.put("wardName", wardName);
        row.put("properties", properties);
        row.put("outstanding", outstanding);
        return row;
    }

    private Map<String, Object> defaulter(String holdingNumber, String ownerName, int wardNumber, String wardName, String locality, int amountDue) {
        Map<String, Object> row = new HashMap<>();
        row.put("holdingNumber", holdingNumber);
        row.put("ownerName", ownerName);
        row.put("wardNumber", wardNumber);
        row.put("wardName", wardName);
        row.put("locality", locality);
        row.put("amountDue", amountDue);
        return row;
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
