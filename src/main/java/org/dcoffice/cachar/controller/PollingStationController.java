package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ClusterResultDTO;
import org.dcoffice.cachar.entity.PollingStation;
import org.dcoffice.cachar.service.PdfRouteService;
import org.dcoffice.cachar.service.PollingStationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polling-stations")
@CrossOrigin(origins = "*")
public class PollingStationController {

    private static final Logger logger =
            LoggerFactory.getLogger(PollingStationController.class);

    private final PollingStationService service;
    private final PdfRouteService pdfRouteService;

    // ✅ constructor EXACTLY as in your current class
    public PollingStationController(
            PollingStationService service,
            PdfRouteService pdfRouteService) {
        this.service = service;
        this.pdfRouteService = pdfRouteService;
    }

    // =========================================================
    // 🔹 EXCEL UPLOAD (RESTORED – BACKWARD COMPATIBLE)
    // =========================================================

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(
            @RequestParam("filePath") String filePath) {

        try {
            logger.info("Uploading polling stations from Excel: {}", filePath);
            service.processExcelFile(filePath, 8); // H3 resolution = 8

            return ResponseEntity.ok(
                    "Polling stations uploaded successfully from: " + filePath
            );
        } catch (Exception e) {
            logger.error("Excel upload failed", e);
            return ResponseEntity.status(500)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // =========================================================
    // 🔹 FETCH APIs (UNCHANGED)
    // =========================================================

    @GetMapping("/byLac/{lacNo}")
    public ResponseEntity<List<PollingStation>> getStationsByLac(
            @PathVariable int lacNo) {

        logger.info("Fetching polling stations for LAC {}", lacNo);
        return ResponseEntity.ok(service.getStationsByLac(lacNo));
    }

    @GetMapping("/byHex/{h3Index}")
    public ResponseEntity<List<PollingStation>> getStationsByHex(
            @PathVariable String h3Index) {

        logger.info("Fetching polling stations for H3 index {}", h3Index);
        return ResponseEntity.ok(service.getStationsByHex(h3Index));
    }

    // =========================================================
    // 🔹 AGGREGATION APIs
    // =========================================================

    @GetMapping("/aggregate/byLac")
    public ResponseEntity<Map<Integer, Long>> getStationCountByLac() {

        logger.info("Aggregated polling station count per LAC");
        return ResponseEntity.ok(service.getStationCountByLac());
    }

    @GetMapping("/aggregate/byHex")
    public ResponseEntity<Map<String, Long>> getStationCountByHex() {

        logger.info("Aggregated polling station count per H3 cell");
        return ResponseEntity.ok(service.getStationCountByHex());
    }

    // =========================================================
    // 🔹 CLUSTER API (USED BY PDF TOO)
    // =========================================================

    @GetMapping("/cluster")
    public ResponseEntity<List<ClusterResultDTO>> clusterStations(
            @RequestParam(required = false) Integer lacNo,
            @RequestParam(defaultValue = "5") int clusterSize) {

        logger.info("Clustering polling stations | LAC={} | clusterSize={}",
                lacNo, clusterSize);

        return ResponseEntity.ok(
                service.clusterPollingStations(lacNo, clusterSize)
        );
    }

    // =========================================================
    // 🔹 ROUTE PLAN PDF (MATCHES YOUR CURRENT SERVICE)
    // =========================================================

    @GetMapping(value = "/routes/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> generateRoutePdf(
            @RequestParam(required = false) Integer lacNo,
            @RequestParam(defaultValue = "5") int clusterSize) {

        logger.info("Generating route PDF | LAC={} | clusterSize={}",
                lacNo, clusterSize);

        // ✅ IMPORTANT: uses existing clustering result
        List<ClusterResultDTO> clusters =
                service.clusterPollingStations(lacNo, clusterSize);

        // ✅ EXACT signature you already have
        byte[] pdf =
                pdfRouteService.generateClusterRoutePdf(
                        lacNo, clusterSize, clusters);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=route-plan.pdf")
                .body(pdf);
    }
}
