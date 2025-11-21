package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.CitizenUpdateRequest;
import org.dcoffice.cachar.dto.OTPRequest;
import org.dcoffice.cachar.entity.Citizen;
import org.dcoffice.cachar.service.CitizenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private org.dcoffice.cachar.service.JwtService jwtService;

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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> loginWithMobile(@Valid @RequestBody OTPRequest request) {
        // Let CitizenNotFoundException and other runtime exceptions propagate
        // so the application's GlobalExceptionHandler can map them to proper HTTP responses.
        citizenService.sendOTPForLogin(request.getMobileNumber());
        return ResponseEntity.ok(ApiResponse.success("Login OTP sent successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> verifyOTP(@Valid @RequestBody OTPRequest request) {
        try {
            org.dcoffice.cachar.entity.Citizen citizen = citizenService.verifyOTPAndGetCitizen(request.getMobileNumber(), request.getOtp());

            if (citizen == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP"));
            }

            // Issue JWT for the verified citizen
            String token = jwtService.generateTokenForCitizen(citizen);
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("token", token);
            data.put("citizenId", citizen.getId());

            return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", data));
        } catch (Exception e) {
            logger.error("OTP verification failed for {}: {}", request.getMobileNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("OTP verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerCitizen(@Valid @RequestBody Citizen citizen) {
        try {
            // Create citizen (or update unverified existing) and send signup OTP.
            Citizen savedCitizen = citizenService.createCitizenAndSendOTP(citizen);
            return ResponseEntity.ok(ApiResponse.success("Signup initiated. OTP sent to mobile number.", savedCitizen.getId()));
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

    // Get current citizen profile (authenticated)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Citizen>> getCurrentCitizenProfile(Authentication authentication) {
        try {
            String citizenId = authentication.getName();
            Optional<Citizen> citizenOpt = citizenService.findById(citizenId);
            if (citizenOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", citizenOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get citizen profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve profile: " + e.getMessage()));
        }
    }

    // Update citizen profile (authenticated)
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Citizen>> updateCitizenProfile(@Valid @RequestBody CitizenUpdateRequest request, Authentication authentication) {
        try {
            String citizenId = authentication.getName();
            Optional<Citizen> existingCitizenOpt = citizenService.findById(citizenId);
            
            if (!existingCitizenOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Create a Citizen object with updated fields
            Citizen updatedCitizen = new Citizen();
            updatedCitizen.setName(request.getName());
            updatedCitizen.setEmail(request.getEmail());
            updatedCitizen.setAddress(request.getAddress());
            updatedCitizen.setPincode(request.getPincode());
            
            Citizen updated = citizenService.updateCitizen(citizenId, updatedCitizen);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
        } catch (Exception e) {
            logger.error("Failed to update citizen profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Profile update failed: " + e.getMessage()));
        }
    }
}