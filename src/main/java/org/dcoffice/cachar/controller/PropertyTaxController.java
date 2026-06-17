package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.LinkPropertyRequest;
import org.dcoffice.cachar.dto.PayPropertyTaxRequest;
import org.dcoffice.cachar.dto.PropertyTaxServiceRequestDto;
import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.dcoffice.cachar.entity.PropertyTaxServiceRequest;
import org.dcoffice.cachar.service.PropertyTaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PropertyTaxController {

    private final PropertyTaxService propertyTaxService;

    public PropertyTaxController(PropertyTaxService propertyTaxService) {
        this.propertyTaxService = propertyTaxService;
    }

    @GetMapping("/property-tax/account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> account(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("SMC account retrieved", propertyTaxService.getCitizenAccount(authentication.getName())));
    }

    @PostMapping("/property-tax/link")
    public ResponseEntity<ApiResponse<PropertyTaxAccount>> linkProperty(
            @Valid @RequestBody LinkPropertyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Property linked to SMC account", propertyTaxService.linkProperty(authentication.getName(), request.getHoldingNumber())));
    }

    @PostMapping("/property-tax/pay")
    public ResponseEntity<ApiResponse<PropertyTaxPaymentReceipt>> pay(
            @Valid @RequestBody PayPropertyTaxRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Property tax payment recorded", propertyTaxService.payPropertyTax(authentication.getName(), request.getHoldingNumber(), request.getPaymentMode())));
    }

    @GetMapping("/property-tax/receipt/{receiptNumber}")
    public ResponseEntity<ApiResponse<PropertyTaxPaymentReceipt>> verifyReceipt(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(ApiResponse.success("Receipt verified", propertyTaxService.verifyReceipt(receiptNumber)));
    }

    @PostMapping("/property-tax/service-request")
    public ResponseEntity<ApiResponse<PropertyTaxServiceRequest>> createServiceRequest(
            @Valid @RequestBody PropertyTaxServiceRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Property service request submitted", propertyTaxService.createServiceRequest(
                authentication.getName(),
                request.getHoldingNumber(),
                request.getRequestType(),
                request.getRemarks()
        )));
    }

    @GetMapping("/officer/property-tax/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> officerDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Property tax dashboard retrieved", propertyTaxService.dashboard()));
    }

    @GetMapping("/officer/property-tax/defaulters")
    public ResponseEntity<ApiResponse<List<PropertyTaxAccount>>> defaulters() {
        return ResponseEntity.ok(ApiResponse.success("Property tax defaulters retrieved", propertyTaxService.defaulters()));
    }
}
