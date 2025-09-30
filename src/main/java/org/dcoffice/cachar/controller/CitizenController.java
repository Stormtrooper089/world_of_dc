package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.OTPRequest;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.service.CitizenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/citizen")
@CrossOrigin(origins = "*")
public class CitizenController {

    private static final Logger logger = LoggerFactory.getLogger(CitizenController.class);

    @Autowired
    private CitizenService citizenService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOTP(@Valid @RequestBody OTPRequest request) {
        try {
            citizenService.sendOTP(request.getMobileNumber());
            return ResponseEntity.ok(ApiResponse.success("OTP sent successfully"));
        } catch (Exception e) {
            logger.error("Failed to send OTP to {}: {}", request.getMobileNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOTP(@Valid @RequestBody OTPRequest request) {
        try {
            boolean isValid = citizenService.verifyOTP(request.getMobileNumber(), request.getOtp());

            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP"));
            }
        } catch (Exception e) {
            logger.error("OTP verification failed for {}: {}", request.getMobileNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("OTP verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerCitizen(@Valid @RequestBody Citizen citizen) {
        try {
            // Verify that mobile number is already verified
            if (!citizenService.isCitizenVerified(citizen.getMobileNumber())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Mobile number not verified. Please verify OTP first."));
            }

            Citizen savedCitizen = citizenService.registerOrUpdateCitizen(citizen);
            return ResponseEntity.ok(ApiResponse.success("Citizen registered successfully", savedCitizen.getId()));
        } catch (Exception e) {
            logger.error("Citizen registration failed for {}: {}", citizen.getMobileNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/profile/{mobileNumber}")
    public ResponseEntity<ApiResponse<Citizen>> getCitizenProfile(@PathVariable String mobileNumber) {
        try {
            Optional<Citizen> citizenOpt = citizenService.findByMobileNumber(mobileNumber);
            if (citizenOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Citizen profile retrieved", citizenOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get citizen profile for {}: {}", mobileNumber, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve profile: " + e.getMessage()));
        }
    }
}