package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.AdminWorkerSummaryDto;
import org.dcoffice.cachar.dto.WorkerUserDto;
import org.dcoffice.cachar.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class WorkerAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WorkerAdminController.class);

    @Autowired
    private WorkerService workerService;

    /** Returns workers scoped to the caller's squad. */
    @GetMapping("/admin/workers")
    public ResponseEntity<?> getWorkers(Authentication authentication) {
        if (!isWorkerAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Forbidden"));
        }
        try {
            List<WorkerUserDto> workers = workerService.getWorkers(authentication.getName());
            return ResponseEntity.ok(workers);
        } catch (Exception e) {
            logger.error("Get workers failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e.getMessage()));
        }
    }

    /** Returns today's attendance scoped to the caller's squad. */
    @GetMapping("/admin/attendance/today")
    public ResponseEntity<?> getTodayAttendance(Authentication authentication) {
        if (!isWorkerAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Forbidden"));
        }
        try {
            List<AdminWorkerSummaryDto> summary = workerService.getSquadTodayAttendance(authentication.getName());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Get today attendance summary failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e.getMessage()));
        }
    }

    /**
     * Designate or remove a member as squad supervisor.
     * PUT /admin/workers/{memberId}/supervisor?enable=true|false
     * Only callable by an existing WORKER_ADMIN.
     */
    @PutMapping("/admin/workers/{memberId}/supervisor")
    public ResponseEntity<?> setSupervisor(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "true") boolean enable,
            Authentication authentication) {
        if (!isWorkerAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Forbidden"));
        }
        try {
            WorkerUserDto updated = workerService.setSupervisor(memberId, enable);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.getMessage()));
        } catch (Exception e) {
            logger.error("Set supervisor failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e.getMessage()));
        }
    }

    private boolean isWorkerAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WORKER_ADMIN".equals(a.getAuthority()));
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return body;
    }
}
