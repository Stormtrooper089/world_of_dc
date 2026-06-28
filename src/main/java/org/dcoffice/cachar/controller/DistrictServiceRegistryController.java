package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.DistrictServiceRequest;
import org.dcoffice.cachar.entity.DistrictService;
import org.dcoffice.cachar.service.DistrictServiceRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DistrictServiceRegistryController {
    private final DistrictServiceRegistryService serviceRegistryService;

    public DistrictServiceRegistryController(DistrictServiceRegistryService serviceRegistryService) {
        this.serviceRegistryService = serviceRegistryService;
    }

    @GetMapping("/service-registry")
    public ResponseEntity<ApiResponse<List<DistrictService>>> publicServices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.success("District services retrieved", serviceRegistryService.publicServices(query, department, category)));
    }

    @GetMapping("/officer/service-registry")
    public ResponseEntity<ApiResponse<List<DistrictService>>> officerServices() {
        return ResponseEntity.ok(ApiResponse.success("Officer service registry retrieved", serviceRegistryService.officerServices()));
    }

    @GetMapping("/officer/service-registry/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success("Service registry dashboard retrieved", serviceRegistryService.dashboard()));
    }

    @PostMapping("/officer/service-registry")
    public ResponseEntity<ApiResponse<DistrictService>> create(@RequestBody DistrictServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("District service created", serviceRegistryService.create(request)));
    }

    @PutMapping("/officer/service-registry/{serviceCode}")
    public ResponseEntity<ApiResponse<DistrictService>> update(@PathVariable String serviceCode, @RequestBody DistrictServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("District service updated", serviceRegistryService.update(serviceCode, request)));
    }
}
