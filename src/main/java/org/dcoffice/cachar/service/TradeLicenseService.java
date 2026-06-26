package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.TradeLicenseApplicationRequest;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.entity.TradeLicense;
import org.dcoffice.cachar.entity.TradeLicenseApplication;
import org.dcoffice.cachar.repository.CitizenRepository;
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
    private final CounterService counterService;

    public TradeLicenseService(
            CitizenRepository citizenRepository,
            TradeLicenseRepository licenseRepository,
            TradeLicenseApplicationRepository applicationRepository,
            CounterService counterService
    ) {
        this.citizenRepository = citizenRepository;
        this.licenseRepository = licenseRepository;
        this.applicationRepository = applicationRepository;
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
        application.setSubmittedAt(LocalDateTime.now());
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
        response.put("pendingApplications", applicationRepository.countByStatus("SUBMITTED"));
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
