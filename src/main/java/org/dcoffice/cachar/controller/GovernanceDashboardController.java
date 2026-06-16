package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.GovernanceDashboardResponse;
import org.dcoffice.cachar.service.GovernanceDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance")
@CrossOrigin(origins = "*")
public class GovernanceDashboardController {
    private final GovernanceDashboardService governanceDashboardService;

    public GovernanceDashboardController(GovernanceDashboardService governanceDashboardService) {
        this.governanceDashboardService = governanceDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<GovernanceDashboardResponse>> getDashboard(
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Governance dashboard retrieved successfully",
                        governanceDashboardService.getDashboard(days)
                )
        );
    }
}
