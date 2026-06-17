package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.dcoffice.cachar.entity.PropertyTaxServiceRequest;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.PropertyTaxServiceRequestRepository;
import org.dcoffice.cachar.service.propertytax.PropertyTaxProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PropertyTaxService {

    private final List<PropertyTaxProvider> providers;
    private final CitizenRepository citizenRepository;
    private final PropertyTaxServiceRequestRepository serviceRequestRepository;
    private final CounterService counterService;
    private final String providerMode;

    public PropertyTaxService(
            List<PropertyTaxProvider> providers,
            CitizenRepository citizenRepository,
            PropertyTaxServiceRequestRepository serviceRequestRepository,
            CounterService counterService,
            @Value("${property-tax.provider:${PROPERTY_TAX_PROVIDER:MOCK}}") String providerMode
    ) {
        this.providers = providers;
        this.citizenRepository = citizenRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.counterService = counterService;
        this.providerMode = providerMode;
    }

    public Map<String, Object> getCitizenAccount(String citizenId) {
        Map<String, Object> account = provider().getCitizenAccount(citizenId);
        account.put("propertyServiceRequests", serviceRequestRepository.findByCitizenIdOrderBySubmittedAtDesc(citizenId));
        return account;
    }

    public PropertyTaxAccount linkProperty(String citizenId, String holdingNumber) {
        return provider().linkProperty(citizenId, holdingNumber);
    }

    public PropertyTaxPaymentReceipt payPropertyTax(String citizenId, String holdingNumber, String paymentMode) {
        return provider().payPropertyTax(citizenId, holdingNumber, paymentMode);
    }

    public PropertyTaxPaymentReceipt verifyReceipt(String receiptNumber) {
        return provider().verifyReceipt(receiptNumber);
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = provider().dashboard();
        dashboard.put("propertyServiceRequests", serviceRequestRepository.findAll());
        return dashboard;
    }

    public List<PropertyTaxAccount> defaulters() {
        return provider().defaulters();
    }

    public PropertyTaxServiceRequest createServiceRequest(String citizenId, String holdingNumber, String requestType, String remarks) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));

        PropertyTaxServiceRequest request = new PropertyTaxServiceRequest();
        request.setRequestNumber("SMC-TAX-REQ-" + String.format("%06d", counterService.getNextSequence("propertyTaxServiceRequest")));
        request.setCitizenId(citizenId);
        request.setSmcCitizenId(citizen.getSmcCitizenId());
        request.setHoldingNumber(holdingNumber == null ? null : holdingNumber.trim().toUpperCase(Locale.ROOT));
        request.setRequestType(requestType);
        request.setApplicantName(citizen.getName());
        request.setMobileNumber(citizen.getMobileNumber());
        request.setRemarks(remarks);
        request.setSubmittedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return serviceRequestRepository.save(request);
    }

    private PropertyTaxProvider provider() {
        String mode = providerMode == null || providerMode.isBlank() ? "MOCK" : providerMode.trim().toUpperCase(Locale.ROOT);
        return providers.stream()
                .filter(provider -> provider.providerCode().equalsIgnoreCase(mode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unsupported property tax provider: " + mode));
    }
}
