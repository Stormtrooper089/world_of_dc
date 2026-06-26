package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.TradeLicenseApplicationRequest;
import org.dcoffice.cachar.dto.TradeLicenseDecisionRequest;
import org.dcoffice.cachar.dto.TradeLicenseFeedbackRequest;
import org.dcoffice.cachar.dto.TradeLicensePaymentRequest;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.Officer;
import org.dcoffice.cachar.entity.TradeLicense;
import org.dcoffice.cachar.entity.TradeLicenseApplication;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.dcoffice.cachar.repository.OfficerRepository;
import org.dcoffice.cachar.repository.TradeLicenseApplicationRepository;
import org.dcoffice.cachar.repository.TradeLicenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TradeLicenseService {
    private final CitizenRepository citizenRepository;
    private final TradeLicenseRepository licenseRepository;
    private final TradeLicenseApplicationRepository applicationRepository;
    private final OfficerRepository officerRepository;
    private final CounterService counterService;

    public TradeLicenseService(
            CitizenRepository citizenRepository,
            TradeLicenseRepository licenseRepository,
            TradeLicenseApplicationRepository applicationRepository,
            OfficerRepository officerRepository,
            CounterService counterService
    ) {
        this.citizenRepository = citizenRepository;
        this.licenseRepository = licenseRepository;
        this.applicationRepository = applicationRepository;
        this.officerRepository = officerRepository;
        this.counterService = counterService;
    }

    public Map<String, Object> getCitizenTradeLicenseAccount(String citizenId) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        ensureSeedLicenses();
        Map<String, Object> account = new HashMap<>();
        List<TradeLicense> licenses = licenseRepository.findByLinkedCitizenId(citizenId);
        List<TradeLicenseApplication> applications = applicationRepository.findByCitizenIdOrderBySubmittedAtDesc(citizenId);

        account.put("citizen", citizen);
        account.put("smcCitizenId", citizen.getSmcCitizenId());
        account.put("tradeLicenses", licenses);
        account.put("tradeLicenseApplications", applications);
        account.put("tradeLicenseDue", licenses.stream()
                .map(TradeLicense::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return account;
    }

    public TradeLicense linkLicense(String citizenId, String licenseNumber) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        ensureSeedLicenses();
        TradeLicense license = licenseRepository.findByLicenseNumber(normalize(licenseNumber))
                .orElseThrow(() -> new IllegalArgumentException("No trade license found for license number " + licenseNumber));

        if (license.getLinkedCitizenId() != null && !license.getLinkedCitizenId().equals(citizenId)) {
            throw new IllegalArgumentException("This trade license is already linked to another SMC account");
        }

        license.setLinkedCitizenId(citizenId);
        license.setSmcCitizenId(citizen.getSmcCitizenId());
        license.setLinkedAt(LocalDateTime.now());
        license.setUpdatedAt(LocalDateTime.now());
        return licenseRepository.save(license);
    }

    public TradeLicenseApplication submitApplication(String citizenId, TradeLicenseApplicationRequest request) {
        Citizen citizen = ensureSmcCitizenId(citizenId);
        TradeLicenseApplication application = new TradeLicenseApplication();
        application.setApplicationNumber("SMC-TL-APP-" + String.format("%06d", counterService.getNextSequence("tradeLicenseApplication")));
        application.setCitizenId(citizenId);
        application.setSmcCitizenId(citizen.getSmcCitizenId());
        application.setLicenseNumber(normalizeNullable(request.getLicenseNumber()));
        application.setApplicationType(request.getApplicationType());
        application.setApplicantName(citizen.getName());
        application.setMobileNumber(citizen.getMobileNumber());
        application.setBusinessName(request.getBusinessName());
        application.setTradeType(request.getTradeType());
        application.setBusinessAddress(request.getBusinessAddress());
        application.setWardNumber(request.getWardNumber());
        application.setLocality(request.getLocality());
        application.setRemarks(request.getRemarks());
        application.setStatus("SUBMITTED");
        application.setPaymentStatus("NOT_REQUIRED");
        application.setProvider("UPYOG_READY");
        application.setUpyogApplicationId("UPYOG-TL-" + application.getApplicationNumber());
        application.setUpyogPaymentConsumerCode(application.getApplicationNumber());
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public TradeLicenseApplication approveApplication(String applicationNumber, String officerId, TradeLicenseDecisionRequest request) {
        TradeLicenseApplication application = findApplication(applicationNumber);
        if (!"SUBMITTED".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalArgumentException("Only submitted trade license applications can be accepted");
        }

        BigDecimal payableAmount = request != null && request.getPayableAmount() != null
                ? request.getPayableAmount()
                : defaultPayableAmount(application);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payable amount cannot be negative");
        }

        Officer officer = officerRepository.findById(officerId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        application.setStatus(payableAmount.compareTo(BigDecimal.ZERO) == 0 ? "APPROVED" : "PAYMENT_PENDING");
        application.setPaymentStatus(payableAmount.compareTo(BigDecimal.ZERO) == 0 ? "NOT_REQUIRED" : "PENDING");
        application.setPayableAmount(payableAmount);
        application.setProcessedByOfficerId(officerId);
        application.setProcessedByOfficerName(officer != null ? officer.getName() : "SMC Officer");
        application.setOfficerRemarks(request == null ? null : request.getRemarks());
        application.setProcessedAt(now);
        application.setProvider("UPYOG_READY");
        application.setUpyogBusinessService("TL");
        if (application.getUpyogApplicationId() == null) {
            application.setUpyogApplicationId("UPYOG-TL-" + application.getApplicationNumber());
        }
        application.setUpyogPaymentConsumerCode(application.getApplicationNumber());
        application.setUpdatedAt(now);
        return applicationRepository.save(application);
    }

    public TradeLicenseApplication rejectApplication(String applicationNumber, String officerId, TradeLicenseDecisionRequest request) {
        TradeLicenseApplication application = findApplication(applicationNumber);
        if (!"SUBMITTED".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalArgumentException("Only submitted trade license applications can be rejected");
        }
        Officer officer = officerRepository.findById(officerId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        application.setStatus("REJECTED");
        application.setPaymentStatus("NOT_REQUIRED");
        application.setProcessedByOfficerId(officerId);
        application.setProcessedByOfficerName(officer != null ? officer.getName() : "SMC Officer");
        application.setOfficerRemarks(request == null ? null : request.getRemarks());
        application.setRejectionReason(request == null ? null : request.getRejectionReason());
        application.setProcessedAt(now);
        application.setUpdatedAt(now);
        return applicationRepository.save(application);
    }

    public TradeLicenseApplication payApplication(String citizenId, String applicationNumber, TradeLicensePaymentRequest request) {
        TradeLicenseApplication application = findApplication(applicationNumber);
        assertCitizenOwnsApplication(citizenId, application);
        if (!"PAYMENT_PENDING".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalArgumentException("Trade license application is not pending payment");
        }

        LocalDateTime now = LocalDateTime.now();
        String receiptNumber = "SMC-TL-RCPT-" + String.format("%06d", counterService.getNextSequence("tradeLicenseReceipt"));
        application.setStatus("ISSUED");
        application.setPaymentStatus("PAID");
        application.setPaymentMode(request == null || request.getPaymentMode() == null ? "UPYOG_SANDBOX" : request.getPaymentMode());
        application.setPaymentReference("UPYOG-PAY-TL-" + receiptNumber);
        application.setReceiptNumber(receiptNumber);
        application.setPaidAt(now);
        application.setUpdatedAt(now);

        TradeLicense license = issueOrRenewLicense(application, now);
        application.setLicenseNumber(license.getLicenseNumber());
        applicationRepository.save(application);
        return application;
    }

    public TradeLicenseApplication submitFeedback(String citizenId, String applicationNumber, TradeLicenseFeedbackRequest request) {
        TradeLicenseApplication application = findApplication(applicationNumber);
        assertCitizenOwnsApplication(citizenId, application);
        if (!"ISSUED".equalsIgnoreCase(application.getStatus()) && !"APPROVED".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalArgumentException("Feedback can be submitted after the license request is approved or issued");
        }
        if (request == null || request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Feedback rating must be between 1 and 5");
        }
        application.setCitizenRating(request.getRating());
        application.setCitizenFeedback(request.getFeedback());
        application.setFeedbackAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public Map<String, Object> dashboard() {
        ensureSeedLicenses();
        List<TradeLicense> licenses = licenseRepository.findAll();
        List<TradeLicenseApplication> applications = applicationRepository.findAll();
        BigDecimal totalDue = licenses.stream()
                .map(TradeLicense::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("totalLicenses", licenses.size());
        response.put("activeLicenses", licenseRepository.countByStatus("ACTIVE"));
        response.put("expiredLicenses", licenseRepository.countByStatus("EXPIRED"));
        response.put("pendingApplications", applications.stream()
                .filter(application -> !"REJECTED".equalsIgnoreCase(application.getStatus())
                        && !"ISSUED".equalsIgnoreCase(application.getStatus()))
                .count());
        response.put("totalDue", totalDue);
        response.put("recentApplications", applications.stream()
                .sorted(Comparator.comparing(TradeLicenseApplication::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .collect(Collectors.toList()));
        response.put("licenses", licenses);
        return response;
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

    private void ensureSeedLicenses() {
        if (licenseRepository.count() > 0) {
            return;
        }
        licenseRepository.save(seed("SMC-TL-1001", "Ashish Unhale", "9090909090", "Unhale Stores", "Retail Shop", 8, "Itkhola", "ACTIVE", 2400));
        licenseRepository.save(seed("SMC-TL-1002", "Rina Das", "9898989898", "Rina Food Corner", "Food Establishment", 12, "Rangirkhari", "ACTIVE", 3200));
        licenseRepository.save(seed("SMC-TL-1003", "Parijat Hotel", "9090909090", "Parijat Hotel", "Hotel / Lodge", 15, "Tarapur", "EXPIRED", 7800));
    }

    private TradeLicense seed(String licenseNumber, String applicantName, String mobileNumber, String businessName,
                              String tradeType, int wardNumber, String locality, String status, int amountDue) {
        TradeLicense license = new TradeLicense();
        license.setLicenseNumber(licenseNumber);
        license.setApplicantName(applicantName);
        license.setMobileNumber(mobileNumber);
        license.setBusinessName(businessName);
        license.setTradeType(tradeType);
        license.setWardNumber(wardNumber);
        license.setWardName("Ward " + wardNumber);
        license.setLocality(locality);
        license.setBusinessAddress(locality + ", Silchar, Cachar");
        license.setValidFrom(LocalDate.now().minusMonths(8));
        license.setValidTo("EXPIRED".equals(status) ? LocalDate.now().minusDays(20) : LocalDate.now().plusMonths(4));
        license.setAnnualFee(BigDecimal.valueOf(amountDue));
        license.setAmountDue(BigDecimal.valueOf(amountDue));
        license.setStatus(status);
        return license;
    }

    private TradeLicenseApplication findApplication(String applicationNumber) {
        return applicationRepository.findByApplicationNumberIgnoreCase(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Trade license application not found"));
    }

    private void assertCitizenOwnsApplication(String citizenId, TradeLicenseApplication application) {
        if (!citizenId.equals(application.getCitizenId())) {
            throw new IllegalArgumentException("This trade license application is not linked to your SMC account");
        }
    }

    private BigDecimal defaultPayableAmount(TradeLicenseApplication application) {
        if ("CLOSURE".equalsIgnoreCase(application.getApplicationType())) {
            return BigDecimal.ZERO;
        }
        if ("RENEWAL".equalsIgnoreCase(application.getApplicationType())) {
            return BigDecimal.valueOf(1800);
        }
        if ("CORRECTION".equalsIgnoreCase(application.getApplicationType())) {
            return BigDecimal.valueOf(250);
        }
        return BigDecimal.valueOf(2500);
    }

    private TradeLicense issueOrRenewLicense(TradeLicenseApplication application, LocalDateTime paidAt) {
        TradeLicense license = null;
        if (application.getLicenseNumber() != null && !application.getLicenseNumber().isBlank()) {
            license = licenseRepository.findByLicenseNumber(normalize(application.getLicenseNumber())).orElse(null);
        }
        if (license == null) {
            license = new TradeLicense();
            license.setLicenseNumber("SMC-TL-" + String.format("%06d", counterService.getNextSequence("tradeLicense")));
            license.setCreatedAt(paidAt);
        }
        license.setLinkedCitizenId(application.getCitizenId());
        license.setSmcCitizenId(application.getSmcCitizenId());
        license.setApplicantName(application.getApplicantName());
        license.setMobileNumber(application.getMobileNumber());
        license.setBusinessName(application.getBusinessName());
        license.setTradeType(application.getTradeType());
        license.setBusinessAddress(application.getBusinessAddress());
        license.setWardNumber(application.getWardNumber());
        license.setWardName(application.getWardNumber() == null ? null : "Ward " + application.getWardNumber());
        license.setLocality(application.getLocality());
        license.setValidFrom(LocalDate.now());
        license.setValidTo(LocalDate.now().plusYears(1));
        license.setAnnualFee(application.getPayableAmount());
        license.setArrears(BigDecimal.ZERO);
        license.setPenalty(BigDecimal.ZERO);
        license.setAmountDue(BigDecimal.ZERO);
        license.setStatus("ACTIVE");
        license.setLinkedAt(paidAt);
        license.setUpdatedAt(paidAt);
        return licenseRepository.save(license);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalize(value);
    }
}
