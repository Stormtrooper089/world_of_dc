package org.dcoffice.cachar.service.propertytax;

import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.PropertyTaxAccountRepository;
import org.dcoffice.cachar.repository.PropertyTaxPaymentReceiptRepository;
import org.dcoffice.cachar.service.CounterService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpyogPropertyTaxProvider implements PropertyTaxProvider {
    private final UpyogClient upyogClient;
    private final CitizenRepository citizenRepository;
    private final PropertyTaxAccountRepository propertyRepository;
    private final PropertyTaxPaymentReceiptRepository receiptRepository;
    private final CounterService counterService;

    public UpyogPropertyTaxProvider(
            UpyogClient upyogClient,
            CitizenRepository citizenRepository,
            PropertyTaxAccountRepository propertyRepository,
            PropertyTaxPaymentReceiptRepository receiptRepository,
            CounterService counterService
    ) {
        this.upyogClient = upyogClient;
        this.citizenRepository = citizenRepository;
        this.propertyRepository = propertyRepository;
        this.receiptRepository = receiptRepository;
        this.counterService = counterService;
    }

    @Override
    public String providerCode() {
        return "UPYOG";
    }

    @Override
    public Map<String, Object> getCitizenAccount(String citizenId) {
        Citizen citizen = ensureCitizen(citizenId);
        List<PropertyTaxAccount> properties = propertyRepository.findByLinkedCitizenId(citizenId);
        List<PropertyTaxPaymentReceipt> receipts = receiptRepository.findByCitizenIdOrderByPaidAtDesc(citizenId);

        Map<String, Object> response = new HashMap<>();
        response.put("provider", providerCode());
        response.put("citizen", citizen);
        response.put("smcCitizenId", citizen.getSmcCitizenId());
        response.put("linkedProperties", properties);
        response.put("paymentReceipts", receipts);
        response.put("totalDue", properties.stream()
                .map(PropertyTaxAccount::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("services", List.of(
                serviceTile("Property Tax", "UPYOG holding lookup, dues and receipt flow", "ACTIVE"),
                serviceTile("Mutation / Transfer", "UPYOG-ready property service request", "ACTIVE"),
                serviceTile("Bifurcation / Amalgamation", "UPYOG-ready split or merge request", "ACTIVE"),
                serviceTile("Receipt Verification", "Verify payment receipt from UPYOG", "ACTIVE")
        ));
        return response;
    }

    @Override
    public PropertyTaxAccount linkProperty(String citizenId, String holdingNumber) {
        Citizen citizen = ensureCitizen(citizenId);
        Map<String, Object> propertyResponse = upyogClient.searchPropertyByHolding(normalize(holdingNumber), citizenId);
        Map<String, Object> demandResponse = upyogClient.searchDemand(normalize(holdingNumber));

        PropertyTaxAccount account = propertyRepository.findByHoldingNumber(normalize(holdingNumber))
                .orElseGet(PropertyTaxAccount::new);
        applyPropertyResponse(account, propertyResponse, demandResponse, holdingNumber, citizen);
        account.setLinkedCitizenId(citizenId);
        account.setLinkedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return propertyRepository.save(account);
    }

    @Override
    public PropertyTaxPaymentReceipt payPropertyTax(String citizenId, String holdingNumber, String paymentMode) {
        Citizen citizen = ensureCitizen(citizenId);
        PropertyTaxAccount account = propertyRepository.findByHoldingNumber(normalize(holdingNumber))
                .orElseThrow(() -> new IllegalArgumentException("Please link this UPYOG property before payment"));

        Map<String, Object> paymentResponse = upyogClient.initiatePayment(account.getHoldingNumber(), citizenId, paymentMode);
        PropertyTaxPaymentReceipt receipt = mapReceipt(paymentResponse, account, citizen, paymentMode);
        receiptRepository.save(receipt);

        if ("PAID".equalsIgnoreCase(receipt.getStatus())) {
            account.setAmountDue(BigDecimal.ZERO);
            account.setStatus("PAID");
            account.setUpdatedAt(LocalDateTime.now());
            propertyRepository.save(account);
        }
        return receipt;
    }

    @Override
    public PropertyTaxPaymentReceipt verifyReceipt(String receiptNumber) {
        Map<String, Object> receiptResponse = upyogClient.verifyReceipt(receiptNumber);
        PropertyTaxPaymentReceipt existing = receiptRepository.findByReceiptNumber(receiptNumber).orElse(null);
        if (existing != null) {
            return existing;
        }
        PropertyTaxPaymentReceipt receipt = new PropertyTaxPaymentReceipt();
        receipt.setReceiptNumber(text(receiptResponse, "receiptNumber", receiptNumber));
        receipt.setHoldingNumber(text(receiptResponse, "holdingNumber", ""));
        receipt.setOwnerName(text(receiptResponse, "ownerName", ""));
        receipt.setFinancialYear(text(receiptResponse, "financialYear", ""));
        receipt.setAmountPaid(amount(receiptResponse, "amountPaid"));
        receipt.setPaymentMode(text(receiptResponse, "paymentMode", "UPYOG"));
        receipt.setTransactionReference(text(receiptResponse, "transactionReference", ""));
        receipt.setStatus(text(receiptResponse, "status", "VERIFIED"));
        receipt.setPaidAt(LocalDateTime.now());
        return receiptRepository.save(receipt);
    }

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> response = upyogClient.dashboard();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("provider", providerCode());
        dashboard.put("totalProperties", number(response, "totalProperties"));
        dashboard.put("linkedProperties", propertyRepository.findAll().stream().filter(p -> p.getLinkedCitizenId() != null).count());
        dashboard.put("paidProperties", number(response, "paidProperties"));
        dashboard.put("dueProperties", number(response, "dueProperties"));
        dashboard.put("totalDemand", amount(response, "totalDemand"));
        dashboard.put("totalCollected", amount(response, "totalCollected"));
        dashboard.put("totalOutstanding", amount(response, "totalOutstanding"));
        dashboard.put("wardWise", response.getOrDefault("wardWise", new ArrayList<>()));
        dashboard.put("recentReceipts", receiptRepository.findAll());
        dashboard.put("defaulters", defaulters());
        return dashboard;
    }

    @Override
    public List<PropertyTaxAccount> defaulters() {
        Map<String, Object> response = upyogClient.defaulters();
        Object items = response.get("defaulters");
        if (!(items instanceof List)) {
            return propertyRepository.findAll();
        }
        List<PropertyTaxAccount> defaulters = new ArrayList<>();
        for (Object item : (List<?>) items) {
            if (item instanceof Map) {
                Map<String, Object> row = (Map<String, Object>) item;
                PropertyTaxAccount account = new PropertyTaxAccount();
                account.setHoldingNumber(text(row, "holdingNumber", ""));
                account.setOwnerName(text(row, "ownerName", ""));
                account.setWardNumber(number(row, "wardNumber").intValue());
                account.setWardName(text(row, "wardName", ""));
                account.setLocality(text(row, "locality", ""));
                account.setAmountDue(amount(row, "amountDue"));
                account.setStatus("DUE");
                defaulters.add(account);
            }
        }
        return defaulters;
    }

    private Citizen ensureCitizen(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));
        if (citizen.getSmcCitizenId() == null || citizen.getSmcCitizenId().isBlank()) {
            citizen.setSmcCitizenId("SMC-CIT-" + String.format("%06d", counterService.getNextSequence("smcCitizenId")));
            citizen.setUpdatedAt(LocalDateTime.now());
            citizen = citizenRepository.save(citizen);
        }
        return citizen;
    }

    private void applyPropertyResponse(PropertyTaxAccount account, Map<String, Object> propertyResponse, Map<String, Object> demandResponse, String holdingNumber, Citizen citizen) {
        account.setHoldingNumber(text(propertyResponse, "holdingNumber", normalize(holdingNumber)));
        account.setAssessmentNumber(text(propertyResponse, "assessmentNumber", text(propertyResponse, "propertyId", "")));
        account.setOwnerName(text(propertyResponse, "ownerName", citizen.getName()));
        account.setMobileNumber(text(propertyResponse, "mobileNumber", citizen.getMobileNumber()));
        account.setWardNumber(number(propertyResponse, "wardNumber").intValue());
        account.setWardName(text(propertyResponse, "wardName", ""));
        account.setLocality(text(propertyResponse, "locality", ""));
        account.setAddress(text(propertyResponse, "address", ""));
        account.setPropertyType(text(propertyResponse, "propertyType", ""));
        account.setUsageType(text(propertyResponse, "usageType", ""));
        account.setFinancialYear(text(demandResponse, "financialYear", ""));
        account.setAnnualTax(amount(demandResponse, "annualTax"));
        account.setArrears(amount(demandResponse, "arrears"));
        account.setPenalty(amount(demandResponse, "penalty"));
        account.setRebate(amount(demandResponse, "rebate"));
        account.setAmountDue(amount(demandResponse, "amountDue"));
        account.setStatus(account.getAmountDue().compareTo(BigDecimal.ZERO) > 0 ? "DUE" : "PAID");
    }

    private PropertyTaxPaymentReceipt mapReceipt(Map<String, Object> response, PropertyTaxAccount account, Citizen citizen, String paymentMode) {
        PropertyTaxPaymentReceipt receipt = new PropertyTaxPaymentReceipt();
        receipt.setReceiptNumber(text(response, "receiptNumber", "UPYOG-RCPT-" + System.currentTimeMillis()));
        receipt.setHoldingNumber(account.getHoldingNumber());
        receipt.setCitizenId(citizen.getId());
        receipt.setSmcCitizenId(citizen.getSmcCitizenId());
        receipt.setOwnerName(account.getOwnerName());
        receipt.setFinancialYear(account.getFinancialYear());
        receipt.setAmountPaid(amount(response, "amountPaid").compareTo(BigDecimal.ZERO) > 0 ? amount(response, "amountPaid") : account.getAmountDue());
        receipt.setPaymentMode(paymentMode == null || paymentMode.isBlank() ? "UPYOG" : paymentMode);
        receipt.setTransactionReference(text(response, "transactionReference", text(response, "transactionId", "")));
        receipt.setStatus(text(response, "status", "PAYMENT_INITIATED"));
        receipt.setPaidAt(LocalDateTime.now());
        return receipt;
    }

    private Map<String, String> serviceTile(String name, String description, String status) {
        Map<String, String> item = new HashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("status", status);
        return item;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String text(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value == null ? fallback : value.toString();
    }

    private Number number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number) return (Number) value;
        if (value == null) return 0;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private BigDecimal amount(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
