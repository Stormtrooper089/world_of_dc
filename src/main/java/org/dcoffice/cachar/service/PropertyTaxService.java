package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.PropertyTaxAccountRepository;
import org.dcoffice.cachar.repository.PropertyTaxPaymentReceiptRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PropertyTaxService {

    private final CitizenRepository citizenRepository;
    private final PropertyTaxAccountRepository propertyRepository;
    private final PropertyTaxPaymentReceiptRepository receiptRepository;
    private final CounterService counterService;

    public PropertyTaxService(
            CitizenRepository citizenRepository,
            PropertyTaxAccountRepository propertyRepository,
            PropertyTaxPaymentReceiptRepository receiptRepository,
            CounterService counterService
    ) {
        this.citizenRepository = citizenRepository;
        this.propertyRepository = propertyRepository;
        this.receiptRepository = receiptRepository;
        this.counterService = counterService;
    }

    public Map<String, Object> getCitizenAccount(String citizenId) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        ensureSeedProperties();
        List<PropertyTaxAccount> properties = propertyRepository.findByLinkedCitizenId(citizenId);
        List<PropertyTaxPaymentReceipt> receipts = receiptRepository.findByCitizenIdOrderByPaidAtDesc(citizenId);

        Map<String, Object> response = new HashMap<>();
        response.put("citizen", citizen);
        response.put("smcCitizenId", citizen.getSmcCitizenId());
        response.put("linkedProperties", properties);
        response.put("paymentReceipts", receipts);
        response.put("totalDue", properties.stream()
                .map(PropertyTaxAccount::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("services", List.of(
                serviceTile("Property Tax", "Link holdings, view dues and receipts", "ACTIVE"),
                serviceTile("Grievances", "Complaints and officer updates", "ACTIVE"),
                serviceTile("Waste Pickup", "Sanitation pickup requests", "ACTIVE"),
                serviceTile("Trade License", "Application tracking foundation", "COMING_SOON")
        ));
        return response;
    }

    public PropertyTaxAccount linkProperty(String citizenId, String holdingNumber) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        ensureSeedProperties();
        PropertyTaxAccount property = propertyRepository.findByHoldingNumber(normalizeHolding(holdingNumber))
                .orElseThrow(() -> new IllegalArgumentException("No property found for holding number " + holdingNumber));

        if (property.getLinkedCitizenId() != null && !property.getLinkedCitizenId().equals(citizenId)) {
            throw new IllegalArgumentException("This property is already linked to another SMC account");
        }

        property.setLinkedCitizenId(citizen.getId());
        property.setLinkedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());
        return propertyRepository.save(property);
    }

    public PropertyTaxPaymentReceipt payPropertyTax(String citizenId, String holdingNumber, String paymentMode) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        PropertyTaxAccount property = propertyRepository.findByHoldingNumber(normalizeHolding(holdingNumber))
                .orElseThrow(() -> new IllegalArgumentException("No property found for holding number " + holdingNumber));

        if (!citizenId.equals(property.getLinkedCitizenId())) {
            throw new IllegalArgumentException("Please link this property to your SMC account before payment");
        }
        if (property.getAmountDue() == null || property.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No pending dues found for this property");
        }

        BigDecimal amountPaid = property.getAmountDue();
        PropertyTaxPaymentReceipt receipt = new PropertyTaxPaymentReceipt();
        receipt.setReceiptNumber(generateReceiptNumber());
        receipt.setHoldingNumber(property.getHoldingNumber());
        receipt.setCitizenId(citizenId);
        receipt.setSmcCitizenId(citizen.getSmcCitizenId());
        receipt.setOwnerName(property.getOwnerName());
        receipt.setFinancialYear(property.getFinancialYear());
        receipt.setAmountPaid(amountPaid);
        receipt.setPaymentMode(paymentMode == null || paymentMode.isBlank() ? "ONLINE_SIMULATION" : paymentMode);
        receipt.setTransactionReference("SMC-SIM-" + System.currentTimeMillis());
        receipt.setPaidAt(LocalDateTime.now());

        property.setAmountDue(BigDecimal.ZERO);
        property.setStatus("PAID");
        property.setUpdatedAt(LocalDateTime.now());
        propertyRepository.save(property);
        return receiptRepository.save(receipt);
    }

    public Map<String, Object> dashboard() {
        ensureSeedProperties();
        List<PropertyTaxAccount> properties = propertyRepository.findAll();
        List<PropertyTaxPaymentReceipt> receipts = receiptRepository.findAll();
        BigDecimal totalDemand = properties.stream()
                .map(property -> safe(property.getAnnualTax()).add(safe(property.getArrears())).add(safe(property.getPenalty())).subtract(safe(property.getRebate())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = properties.stream()
                .map(PropertyTaxAccount::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = receipts.stream()
                .map(PropertyTaxPaymentReceipt::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Integer, Map<String, Object>> wardMap = new HashMap<>();
        for (PropertyTaxAccount property : properties) {
            Integer ward = property.getWardNumber() == null ? 0 : property.getWardNumber();
            wardMap.putIfAbsent(ward, new HashMap<>());
            Map<String, Object> row = wardMap.get(ward);
            row.put("wardNumber", ward);
            row.put("wardName", property.getWardName());
            row.put("properties", ((Number) row.getOrDefault("properties", 0)).intValue() + 1);
            row.put("outstanding", ((BigDecimal) row.getOrDefault("outstanding", BigDecimal.ZERO)).add(safe(property.getAmountDue())));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalProperties", properties.size());
        response.put("linkedProperties", properties.stream().filter(p -> p.getLinkedCitizenId() != null).count());
        response.put("paidProperties", propertyRepository.countByStatus("PAID"));
        response.put("dueProperties", properties.stream().filter(p -> safe(p.getAmountDue()).compareTo(BigDecimal.ZERO) > 0).count());
        response.put("totalDemand", totalDemand);
        response.put("totalCollected", collected);
        response.put("totalOutstanding", outstanding);
        response.put("wardWise", wardMap.values().stream()
                .sorted(Comparator.comparing(row -> ((Number) row.get("wardNumber")).intValue()))
                .collect(Collectors.toList()));
        response.put("recentReceipts", receipts.stream()
                .sorted(Comparator.comparing(PropertyTaxPaymentReceipt::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList()));
        return response;
    }

    public void ensureSeedProperties() {
        if (propertyRepository.count() > 0) {
            return;
        }
        propertyRepository.save(seed("SMC-HLD-1001", "SMC-ASMT-2026-1001", "Ashish Unhale", "9090909090", 8, "Ward 8 - Itkhola", "Itkhola", "Residential", "Self Occupied", 2450, 500, 50, 100));
        propertyRepository.save(seed("SMC-HLD-1002", "SMC-ASMT-2026-1002", "Rina Das", "9898989898", 12, "Ward 12 - Rangirkhari", "Rangirkhari", "Commercial", "Shop", 5200, 1200, 200, 0));
        propertyRepository.save(seed("SMC-HLD-1003", "SMC-ASMT-2026-1003", "Parijat Hotel", "9090909090", 15, "Ward 15 - Tarapur", "Tarapur", "Commercial", "Hotel", 12800, 2500, 750, 0));
    }

    private Citizen ensureSmcCitizenId(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));
        if (citizen.getSmcCitizenId() == null || citizen.getSmcCitizenId().isBlank()) {
            citizen.setSmcCitizenId("SMC-CIT-" + String.format("%06d", counterService.getNextSequence("smcCitizenId")));
            citizen.setUpdatedAt(LocalDateTime.now());
            citizen = citizenRepository.save(citizen);
        }
        return citizen;
    }

    private PropertyTaxAccount seed(String holdingNumber, String assessmentNumber, String ownerName, String mobileNumber,
                                    int wardNumber, String wardName, String locality, String propertyType, String usageType,
                                    int annualTax, int arrears, int penalty, int rebate) {
        PropertyTaxAccount property = new PropertyTaxAccount();
        property.setHoldingNumber(holdingNumber);
        property.setAssessmentNumber(assessmentNumber);
        property.setOwnerName(ownerName);
        property.setMobileNumber(mobileNumber);
        property.setWardNumber(wardNumber);
        property.setWardName(wardName);
        property.setLocality(locality);
        property.setAddress(locality + ", Silchar, Cachar");
        property.setPropertyType(propertyType);
        property.setUsageType(usageType);
        property.setFinancialYear("2026-27");
        property.setAnnualTax(BigDecimal.valueOf(annualTax));
        property.setArrears(BigDecimal.valueOf(arrears));
        property.setPenalty(BigDecimal.valueOf(penalty));
        property.setRebate(BigDecimal.valueOf(rebate));
        property.setAmountDue(BigDecimal.valueOf(annualTax + arrears + penalty - rebate));
        property.setDueDate(LocalDate.of(2026, 9, 30));
        return property;
    }

    private Map<String, String> serviceTile(String name, String description, String status) {
        Map<String, String> item = new HashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("status", status);
        return item;
    }

    private String generateReceiptNumber() {
        return "SMC-TAX-RCPT-" + String.format("%06d", counterService.getNextSequence("propertyTaxReceipt"));
    }

    private String normalizeHolding(String holdingNumber) {
        return holdingNumber == null ? "" : holdingNumber.trim().toUpperCase();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
