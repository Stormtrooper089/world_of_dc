package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.dcoffice.cachar.entity.PropertyTaxServiceRequest;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.PropertyTaxAccountRepository;
import org.dcoffice.cachar.repository.PropertyTaxServiceRequestRepository;
import org.dcoffice.cachar.service.propertytax.PropertyTaxProvider;
import org.dcoffice.cachar.service.propertytax.UpyogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PropertyTaxService {

    private final List<PropertyTaxProvider> providers;
    private final CitizenRepository citizenRepository;
    private final PropertyTaxAccountRepository propertyRepository;
    private final PropertyTaxServiceRequestRepository serviceRequestRepository;
    private final CounterService counterService;
    private final UpyogClient upyogClient;
    private final String providerMode;

    public PropertyTaxService(
            List<PropertyTaxProvider> providers,
            CitizenRepository citizenRepository,
            PropertyTaxAccountRepository propertyRepository,
            PropertyTaxServiceRequestRepository serviceRequestRepository,
            CounterService counterService,
            UpyogClient upyogClient,
            @Value("${property-tax.provider:${PROPERTY_TAX_PROVIDER:UPYOG}}") String providerMode
    ) {
        this.providers = providers;
        this.citizenRepository = citizenRepository;
        this.propertyRepository = propertyRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.counterService = counterService;
        this.upyogClient = upyogClient;
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

    /**
     * Returns municipal-to-land-record linkage metadata. It deliberately does not
     * infer legal parcel boundaries when an authoritative cadastral source has not
     * verified a link yet.
     */
    public Map<String, Object> getLandParcel(String citizenId, String holdingNumber) {
        String normalizedHolding = holdingNumber == null ? "" : holdingNumber.trim().toUpperCase(Locale.ROOT);
        PropertyTaxAccount property = propertyRepository.findByHoldingNumber(normalizedHolding)
                .orElseThrow(() -> new IllegalArgumentException("No property found for holding number " + holdingNumber));
        if (!citizenId.equals(property.getLinkedCitizenId())) {
            throw new IllegalArgumentException("Please link this property to your SMC account before viewing its land record");
        }

        String status = property.getLandRecordStatus() == null || property.getLandRecordStatus().isBlank()
                ? "NOT_LINKED" : property.getLandRecordStatus();
        boolean officialBoundary = "OFFICIAL_BOUNDARY_VERIFIED".equalsIgnoreCase(status);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("holdingNumber", property.getHoldingNumber());
        response.put("assessmentNumber", property.getAssessmentNumber());
        response.put("status", status);
        response.put("cadastralReference", property.getCadastralReference());
        response.put("dagNumber", property.getDagNumber());
        response.put("pattaNumber", property.getPattaNumber());
        response.put("ulpin", property.getUlpin());
        response.put("mapSource", property.getMapSource());
        response.put("officialRecordUrl", property.getOfficialRecordUrl());
        response.put("landRecordUpdatedAt", property.getLandRecordUpdatedAt());
        response.put("officialBoundary", officialBoundary);
        response.put("mapAvailable", officialBoundary);
        response.put("disclaimer", officialBoundary
                ? "Boundary supplied by the linked authorised cadastral record. The certified survey record remains the legal reference."
                : "No authoritative cadastral boundary is linked to this municipal property yet. This view must not be used as a survey or ownership record.");
        return response;
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
        if ("UPYOG".equalsIgnoreCase(providerMode)) {
            upyogClient.createServiceRequest(citizenId, request.getHoldingNumber(), requestType, remarks);
        }
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
