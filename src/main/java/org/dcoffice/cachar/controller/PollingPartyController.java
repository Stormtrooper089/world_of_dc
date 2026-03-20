package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.dto.PollingPartyOptionsDto;
import org.dcoffice.cachar.entity.PollingParty;
import org.dcoffice.cachar.service.PollingPartyExcelService;
import org.dcoffice.cachar.service.PollingPartyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
            @RequestParam(value = "partyNo", required = false) String partyNo,
            @RequestParam(value = "mobile", required = false) String mobile) {
        try {
            List<PollingParty> pollingParties = pollingPartyService.searchPollingParties(psName, partyNo, mobile);
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
}
