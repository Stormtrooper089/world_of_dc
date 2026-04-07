package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.PollingPartyOptionsDto;
import org.dcoffice.cachar.entity.Materials;
import org.dcoffice.cachar.entity.Member;
import org.dcoffice.cachar.entity.PollingParty;
import org.dcoffice.cachar.service.PollingPartyExcelService;
import org.dcoffice.cachar.service.PollingPartyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/polling-parties", "/api/polling-party"})
@CrossOrigin(origins = "*")
public class PollingPartyController {

    private static final Logger logger = LoggerFactory.getLogger(PollingPartyController.class);

    private final PollingPartyService pollingPartyService;
    private final PollingPartyExcelService pollingPartyExcelService;

    public PollingPartyController(PollingPartyService pollingPartyService,
                                  PollingPartyExcelService pollingPartyExcelService) {
        this.pollingPartyService = pollingPartyService;
        this.pollingPartyExcelService = pollingPartyExcelService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PollingParty>>> searchPollingParties(
            @RequestParam(value = "psName", required = false) String psName,
            @RequestParam(value = "mobile", required = false) String mobile) {
        try {
            List<PollingParty> pollingParties = pollingPartyService.searchPollingParties(psName, mobile);
            return ResponseEntity.ok(ApiResponse.success("Polling parties retrieved successfully", pollingParties));
        } catch (Exception e) {
            logger.error("Failed to search polling parties: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to search polling parties: " + e.getMessage()));
        }
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<PollingPartyOptionsDto>> getPollingPartyOptions() {
        try {
            PollingPartyOptionsDto options = new PollingPartyOptionsDto(
                    pollingPartyService.getAllPollingStations(),
                    pollingPartyService.getAllPartyNames());
            return ResponseEntity.ok(ApiResponse.success("Polling party options retrieved successfully", options));
        } catch (Exception e) {
            logger.error("Failed to retrieve polling party options: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve polling party options: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload a valid Excel file");
        }

        try {
            logger.info("Uploading ENCORE Excel: {}", file.getOriginalFilename());

            pollingPartyExcelService.uploadExcel(file.getInputStream());

            return ResponseEntity.ok(
                    "Excel uploaded successfully to MongoDB");

        } catch (Exception e) {
            logger.error("Upload failed", e);

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 📦 GET /api/polling-parties/materials?psName=...
    // ─────────────────────────────────────────────────────────────
//    @GetMapping("/materials")
//    public ResponseEntity<ApiResponse<java.util.List<Materials>>> getMaterials(
//            @RequestParam String psName) {
//        try {
//            java.util.List<Materials> materials = pollingPartyService.getMaterialsByPsName(psName);
//            return ResponseEntity.ok(ApiResponse.success("Materials fetched successfully", materials));
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(ApiResponse.error(e.getMessage()));
//        } catch (Exception e) {
//            logger.error("Failed to fetch materials for psName {}: {}", psName, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Failed to fetch materials: " + e.getMessage()));
//        }
//    }

    // ─────────────────────────────────────────────────────────────
    // ✏️ PUT /api/polling-parties/materials?psName=...
    // ─────────────────────────────────────────────────────────────
//    @PutMapping("/materials")
//    public ResponseEntity<ApiResponse<java.util.List<Materials>>> updateMaterials(
//            @RequestParam String psName,
//            @RequestBody Materials materials) {
//        try {
//            java.util.List<Materials> updated = pollingPartyService.updateMaterialsByPsName(psName, materials);
//            return ResponseEntity.ok(ApiResponse.success("Materials updated successfully", updated));
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(ApiResponse.error(e.getMessage()));
//        } catch (Exception e) {
//            logger.error("Failed to update materials for psName {}: {}", psName, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Failed to update materials: " + e.getMessage()));
//        }
//    }

    // ─────────────────────────────────────────────────────────────
    // 👥 GET /api/polling-parties/members?psName=...
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<java.util.List<Member>>> getMembers(
            @RequestParam String psName) {
        try {
            java.util.List<Member> members = pollingPartyService.getMembersByPsName(psName);
            return ResponseEntity.ok(ApiResponse.success("Members fetched successfully", members));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch members for psName {}: {}", psName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch members: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✏️ PUT /api/polling-parties/members?psName=...
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/members")
    public ResponseEntity<ApiResponse<java.util.List<Member>>> updateMembers(
            @RequestParam String psName,
            @RequestBody java.util.List<Member> members) {
        try {
            java.util.List<Member> updated = pollingPartyService.updateMembersByPsName(psName, members);
            return ResponseEntity.ok(ApiResponse.success("Members updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update members for psName {}: {}", psName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update members: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔑 GET /api/polling-parties/vehicle-id?psName=...
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/vehicle-id")
    public ResponseEntity<ApiResponse<String>> getVehicleId(
            @RequestParam String psName) {
        try {
            String vehicleId = pollingPartyService.getVehicleIdByPsName(psName);
            return ResponseEntity.ok(ApiResponse.success("Vehicle ID fetched successfully", vehicleId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch vehicle ID for psName {}: {}", psName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch vehicle ID: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✏️ PUT /api/polling-parties/vehicle-id?psName=...
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/vehicle-id")
    public ResponseEntity<ApiResponse<String>> updateVehicleId(
            @RequestParam String psName,
            @RequestBody java.util.Map<String, String> request) {
        try {
            String vehicleId = request.get("vehicleId");
            if (vehicleId == null || vehicleId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("vehicleId is required in request body"));
            }
            String updated = pollingPartyService.updateVehicleIdByPsName(psName, vehicleId);
            return ResponseEntity.ok(ApiResponse.success("Vehicle ID updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update vehicle ID for psName {}: {}", psName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update vehicle ID: " + e.getMessage()));
        }
    }
}
