package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.DistrictServiceRequest;
import org.dcoffice.cachar.entity.DistrictService;
import org.dcoffice.cachar.repository.DistrictServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DistrictServiceRegistryService {
    private final DistrictServiceRepository repository;

    public DistrictServiceRegistryService(DistrictServiceRepository repository) {
        this.repository = repository;
    }

    public List<DistrictService> publicServices(String query, String department, String category) {
        ensureSeedServices();
        String q = normalize(query);
        String dept = normalize(department);
        String cat = normalize(category);
        return repository.findByActiveTrueOrderByDisplayOrderAscServiceNameAsc().stream()
                .filter(service -> q.isEmpty() || contains(service.getServiceName(), q)
                        || contains(service.getDescription(), q)
                        || contains(service.getDepartment(), q)
                        || contains(service.getCategory(), q))
                .filter(service -> dept.isEmpty() || normalize(service.getDepartment()).equals(dept))
                .filter(service -> cat.isEmpty() || normalize(service.getCategory()).equals(cat))
                .collect(Collectors.toList());
    }

    public List<DistrictService> officerServices() {
        ensureSeedServices();
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(DistrictService::getDisplayOrder)
                        .thenComparing(DistrictService::getServiceName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public DistrictService create(DistrictServiceRequest request) {
        ensureSeedServices();
        DistrictService service = new DistrictService();
        service.setServiceCode("SMC-SVC-" + System.currentTimeMillis());
        apply(request, service);
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        return repository.save(service);
    }

    public DistrictService update(String serviceCode, DistrictServiceRequest request) {
        DistrictService service = repository.findByServiceCode(serviceCode)
                .orElseThrow(() -> new IllegalArgumentException("District service not found"));
        apply(request, service);
        service.setUpdatedAt(LocalDateTime.now());
        return repository.save(service);
    }

    public Map<String, Object> dashboard() {
        ensureSeedServices();
        List<DistrictService> services = repository.findAll();
        Map<String, Long> departmentWise = services.stream()
                .collect(Collectors.groupingBy(DistrictService::getDepartment, Collectors.counting()));
        Map<String, Long> categoryWise = services.stream()
                .collect(Collectors.groupingBy(DistrictService::getCategory, Collectors.counting()));

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalServices", services.size());
        dashboard.put("activeServices", services.stream().filter(DistrictService::isActive).count());
        dashboard.put("onlineServices", services.stream()
                .filter(service -> contains(service.getServiceMode(), "online"))
                .count());
        dashboard.put("upyogReadyServices", services.stream()
                .filter(service -> contains(service.getIntegrationStatus(), "upyog"))
                .count());
        dashboard.put("departmentWise", departmentWise);
        dashboard.put("categoryWise", categoryWise);
        dashboard.put("services", officerServices());
        return dashboard;
    }

    private void apply(DistrictServiceRequest request, DistrictService service) {
        if (request.getServiceName() != null) service.setServiceName(request.getServiceName());
        if (request.getDepartment() != null) service.setDepartment(request.getDepartment());
        if (request.getCategory() != null) service.setCategory(request.getCategory());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getEligibility() != null) service.setEligibility(request.getEligibility());
        if (request.getRequiredDocuments() != null) service.setRequiredDocuments(request.getRequiredDocuments());
        if (request.getFeeDescription() != null) service.setFeeDescription(request.getFeeDescription());
        if (request.getSlaTimeline() != null) service.setSlaTimeline(request.getSlaTimeline());
        if (request.getServiceMode() != null) service.setServiceMode(request.getServiceMode());
        if (request.getApplyUrl() != null) service.setApplyUrl(request.getApplyUrl());
        if (request.getTrackingUrl() != null) service.setTrackingUrl(request.getTrackingUrl());
        if (request.getEscalationOfficer() != null) service.setEscalationOfficer(request.getEscalationOfficer());
        if (request.getEscalationContact() != null) service.setEscalationContact(request.getEscalationContact());
        if (request.getIntegrationStatus() != null) service.setIntegrationStatus(request.getIntegrationStatus());
        if (request.getActive() != null) service.setActive(request.getActive());
        if (request.getDisplayOrder() != null) service.setDisplayOrder(request.getDisplayOrder());
    }

    private void ensureSeedServices() {
        if (repository.count() > 0) {
            return;
        }
        repository.save(seed("SMC-SVC-GRV", "File Civic Grievance", "Public Grievance Cell", "Citizen Services",
                "Submit ward-tagged complaints for roads, drains, streetlights, encroachment, sanitation and other municipal issues.",
                List.of("Resident or stakeholder of Silchar", "Valid mobile number"),
                List.of("Issue description", "Locality or landmark", "Photo evidence where available", "GPS location where available"),
                "No fee", "3 to 7 working days depending on priority", "Online",
                "/citizen", "/citizen", "Department Officer / Ward Officer", "Track by complaint ID", "Native SMC workflow", 1));
        repository.save(seed("SMC-SVC-WASTE", "Waste Pickup Request", "Sanitation & Solid Waste", "Sanitation",
                "Request pickup for uncollected household waste, bulk waste, drain silt, market waste or public health risk cases.",
                List.of("Citizen with pickup location in SMC area"),
                List.of("Ward number", "Address and landmark", "Before-pickup photo", "GPS coordinates if available"),
                "No fee for civic sanitation requests", "6 to 48 hours based on urgency", "Online + field verification",
                "/citizen", "/citizen", "Sanitation Supervisor", "Conservancy: 03842-233839", "Native SMC workflow", 2));
        repository.save(seed("SMC-SVC-PT", "Property Tax & Holding Services", "Taxation", "Revenue Services",
                "Link property holding, view dues, submit payment and raise mutation or correction requests through My SMC Account.",
                List.of("Property owner or authorised occupant", "SMC Citizen ID"),
                List.of("Holding number", "Mobile number", "Ownership or correction documents where required"),
                "As per assessed property dues", "Payment receipt instant; service requests as per verification", "Online + UPYOG-ready",
                "/my-smc-account", "/my-smc-account", "Taxation Officer", "Office: 03842-233828", "UPYOG-ready adapter", 3));
        repository.save(seed("SMC-SVC-TL", "Trade License Services", "Trade License", "Business Services",
                "Apply for new trade license, renewal, correction or closure with acceptance, payment and citizen feedback workflow.",
                List.of("Business operating or proposed within SMC limits", "SMC Citizen ID"),
                List.of("Business address", "Trade type", "Ward/locality", "Existing license number for renewal or correction"),
                "As approved by licensing officer", "Officer acceptance followed by payment and issuance", "Online + UPYOG-ready",
                "/my-smc-account", "/my-smc-account", "License Officer", "Office: 03842-233828", "UPYOG-ready adapter", 4));
        repository.save(seed("SMC-SVC-AUC", "Municipal Auctions", "Revenue & Auctions", "Revenue Services",
                "View municipal resource auctions, place bids and track award outcomes after SMC verification.",
                List.of("Registered citizen or business bidder", "Accepted auction terms"),
                List.of("Bidder details", "Business details where applicable", "Trade license/GST where required"),
                "As per auction notice and EMD terms", "As per auction schedule", "Online",
                "/auctions", "/auctions", "Revenue Officer", "Auction dashboard", "Native SMC workflow", 5));
        repository.save(seed("SMC-SVC-FIELD", "Field Verification & Squad Monitoring", "Field Administration", "Officer Services",
                "Geo-tagged attendance, work photos and squad movement visibility for field-level municipal governance.",
                List.of("Authorised SMC field staff or officer"),
                List.of("Officer/staff login", "GPS location", "Selfie/work photo where applicable"),
                "Internal service", "Same-day attendance and field reporting", "Officer workflow",
                "/officer-dashboard", "/officer-dashboard", "Administrative Officer", "Officer dashboard", "Native SMC workflow", 6));
    }

    private DistrictService seed(String code, String name, String department, String category, String description,
                                 List<String> eligibility, List<String> documents, String fee, String sla,
                                 String mode, String applyUrl, String trackingUrl, String officer, String contact,
                                 String integration, int order) {
        DistrictService service = new DistrictService();
        service.setServiceCode(code);
        service.setServiceName(name);
        service.setDepartment(department);
        service.setCategory(category);
        service.setDescription(description);
        service.setEligibility(eligibility);
        service.setRequiredDocuments(documents);
        service.setFeeDescription(fee);
        service.setSlaTimeline(sla);
        service.setServiceMode(mode);
        service.setApplyUrl(applyUrl);
        service.setTrackingUrl(trackingUrl);
        service.setEscalationOfficer(officer);
        service.setEscalationContact(contact);
        service.setIntegrationStatus(integration);
        service.setDisplayOrder(order);
        return service;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
    }
}
