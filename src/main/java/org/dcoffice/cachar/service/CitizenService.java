package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.exception.CitizenNotFoundException;
import org.dcoffice.cachar.exception.InvalidOTPException;
import org.dcoffice.cachar.repository.CitizenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class CitizenService {

    private static final Logger logger = LoggerFactory.getLogger(CitizenService.class);

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private OTPService otpService;

    public Citizen registerOrUpdateCitizen(Citizen citizen) {
        Optional<Citizen> existingCitizen = citizenRepository.findByMobileNumber(citizen.getMobileNumber());

        if (existingCitizen.isPresent()) {
            Citizen existing = existingCitizen.get();
            existing.setName(citizen.getName());
            existing.setEmail(citizen.getEmail());
            existing.setAddress(citizen.getAddress());
            existing.setAadharNumber(citizen.getAadharNumber());
            logger.info("Updated citizen profile for mobile number: {}", citizen.getMobileNumber());
            return citizenRepository.save(existing);
        } else {
            logger.info("Registered new citizen with mobile number: {}", citizen.getMobileNumber());
            return citizenRepository.save(citizen);
        }
    }

    public void sendOTP(String mobileNumber) {
        Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);
        Citizen citizen;

        if (citizenOpt.isPresent()) {
            citizen = citizenOpt.get();
        } else {
            citizen = new Citizen();
            citizen.setMobileNumber(mobileNumber);
        }

        String otp = otpService.generateOTP();
        citizen.setOtp(otp);
        citizen.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        citizen.setVerified(false);

        citizenRepository.save(citizen);
        otpService.sendOTP(mobileNumber, otp);

        logger.info("OTP sent successfully to mobile number: {}", mobileNumber);
    }

    /**
     * Send OTP for login only if the citizen is already registered.
     * Does not auto-create a citizen record.
     */
    public void sendOTPForLogin(String mobileNumber) {
        Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);

        if (!citizenOpt.isPresent()) {
            throw new CitizenNotFoundException("Citizen not found with mobile number: " + mobileNumber);
        }

        Citizen citizen = citizenOpt.get();
        String otp = otpService.generateOTP();
        citizen.setOtp(otp);
        citizen.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        // Do not change the verified flag for login

        citizenRepository.save(citizen);
        otpService.sendOTP(mobileNumber, otp);

        logger.info("Login OTP sent successfully to mobile number: {}", mobileNumber);
    }

    public boolean verifyOTP(String mobileNumber, String otp) {
        if (!otpService.isValidOTPFormat(otp)) {
            throw new InvalidOTPException("Invalid OTP format");
        }

        Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);

        if (citizenOpt.isPresent()) {
            Citizen citizen = citizenOpt.get();
            if (citizen.getOtp() != null &&
                    citizen.getOtp().equals(otp) &&
                    citizen.getOtpExpiry() != null &&
                    citizen.getOtpExpiry().isAfter(LocalDateTime.now())) {

                citizen.setVerified(true);
                citizen.setOtp(null);
                citizen.setOtpExpiry(null);
                citizenRepository.save(citizen);

                logger.info("OTP verified successfully for mobile number: {}", mobileNumber);
                return true;
            }
        }

        logger.warn("OTP verification failed for mobile number: {}", mobileNumber);
        return false;
    }

    /**
     * Verify OTP and return the Citizen on success (with verified=true persisted).
     * Returns null when OTP is invalid or expired. Throws InvalidOTPException for format errors.
     */
    public Citizen verifyOTPAndGetCitizen(String mobileNumber, String otp) {
        if (!otpService.isValidOTPFormat(otp)) {
            throw new InvalidOTPException("Invalid OTP format");
        }

        Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(mobileNumber);

        if (citizenOpt.isPresent()) {
            Citizen citizen = citizenOpt.get();
            if (citizen.getOtp() != null &&
                    citizen.getOtp().equals(otp) &&
                    citizen.getOtpExpiry() != null &&
                    citizen.getOtpExpiry().isAfter(LocalDateTime.now())) {

                citizen.setVerified(true);
                citizen.setOtp(null);
                citizen.setOtpExpiry(null);
                citizenRepository.save(citizen);

                logger.info("OTP verified successfully for mobile number: {}", mobileNumber);
                return citizen;
            }
        }

        logger.warn("OTP verification failed for mobile number: {}", mobileNumber);
        return null;
    }

    public Optional<Citizen> findByMobileNumber(String mobileNumber) {
        return citizenRepository.findByMobileNumber(mobileNumber);
    }

    public Citizen getCitizenByMobileNumber(String mobileNumber) {
        return citizenRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new CitizenNotFoundException("Citizen not found with mobile number: " + mobileNumber));
    }

    public Optional<Citizen> findById(String id) {
        return citizenRepository.findById(id);
    }

    public boolean isCitizenVerified(String mobileNumber) {
        return citizenRepository.findByMobileNumber(mobileNumber)
                .map(Citizen::isVerified)
                .orElse(false);
    }

    public Long getTotalVerifiedCitizens() {
        return citizenRepository.countVerifiedCitizens();
    }

    /**
     * Update citizen profile information
     */
    public Citizen updateCitizen(String citizenId, Citizen updatedCitizen) {
        Optional<Citizen> existingCitizenOpt = citizenRepository.findById(citizenId);
        if (!existingCitizenOpt.isPresent()) {
            throw new CitizenNotFoundException("Citizen not found with id: " + citizenId);
        }

        Citizen existing = existingCitizenOpt.get();
        
        // Update only the allowed fields
        if (updatedCitizen.getName() != null && !updatedCitizen.getName().trim().isEmpty()) {
            existing.setName(updatedCitizen.getName().trim());
        }
        if (updatedCitizen.getEmail() != null && !updatedCitizen.getEmail().trim().isEmpty()) {
            existing.setEmail(updatedCitizen.getEmail().trim());
        }
        if (updatedCitizen.getAddress() != null) {
            existing.setAddress(updatedCitizen.getAddress().trim());
        }
        if (updatedCitizen.getPincode() != null && !updatedCitizen.getPincode().trim().isEmpty()) {
            existing.setPincode(updatedCitizen.getPincode().trim());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        logger.info("Updated citizen profile for id: {}", citizenId);
        return citizenRepository.save(existing);
    }

    /**
     * Create or update a citizen as part of signup and send OTP.
     * If a verified citizen already exists with the same mobile number, throws IllegalArgumentException.
     */
    public Citizen createCitizenAndSendOTP(Citizen citizen) {
        Optional<Citizen> existingCitizen = citizenRepository.findByMobileNumber(citizen.getMobileNumber());

        if (existingCitizen.isPresent()) {
            Citizen existing = existingCitizen.get();
            if (existing.isVerified()) {
                throw new IllegalArgumentException("Mobile number already registered");
            }

            // Update details for unverified existing citizen
            existing.setName(citizen.getName());
            existing.setEmail(citizen.getEmail());
            existing.setAddress(citizen.getAddress());
            existing.setAadharNumber(citizen.getAadharNumber());

            String otp = otpService.generateOTP();
            existing.setOtp(otp);
            existing.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            existing.setVerified(false);

            citizenRepository.save(existing);
            otpService.sendOTP(existing.getMobileNumber(), otp);

            logger.info("Signup OTP re-sent to existing unverified mobile: {}", existing.getMobileNumber());
            return existing;
        } else {
            // New citizen: set not verified and send OTP
            citizen.setVerified(false);
            String otp = otpService.generateOTP();
            citizen.setOtp(otp);
            citizen.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

            Citizen saved = citizenRepository.save(citizen);
            otpService.sendOTP(saved.getMobileNumber(), otp);

            logger.info("Signup OTP sent to new mobile: {}", saved.getMobileNumber());
            return saved;
        }
    }
}