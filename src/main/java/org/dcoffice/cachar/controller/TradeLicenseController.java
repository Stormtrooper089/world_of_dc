package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.LinkTradeLicenseRequest;
import org.dcoffice.cachar.dto.TradeLicenseApplicationRequest;
import org.dcoffice.cachar.entity.TradeLicense;
import org.dcoffice.cachar.entity.TradeLicenseApplication;
import org.dcoffice.cachar.service.TradeLicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TradeLicenseController {
    private final TradeLicenseService tradeLicenseService;

    public TradeLicenseController(TradeLicenseService tradeLicenseService) {
        this.tradeLicenseService = tradeLicenseService;
    }

    @GetMapping("/trade-license/account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> account(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Trade license account retrieved", tradeLicenseService.getCitizenTradeLicenseAccount(authentication.getName())));
    }

    @PostMapping("/trade-license/link")
    public ResponseEntity<ApiResponse<TradeLicense>> linkLicense(
            @Valid @RequestBody LinkTradeLicenseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Trade license linked to SMC account", tradeLicenseService.linkLicense(authentication.getName(), request.getLicenseNumber())));
    }

    @PostMapping("/trade-license/application")
    public ResponseEntity<ApiResponse<TradeLicenseApplication>> submitApplication(
            @Valid @RequestBody TradeLicenseApplicationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Trade license application submitted", tradeLicenseService.submitApplication(authentication.getName(), request)));
    }

    @GetMapping("/officer/trade-license/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> officerDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Trade license dashboard retrieved", tradeLicenseService.dashboard()));
    }
}
