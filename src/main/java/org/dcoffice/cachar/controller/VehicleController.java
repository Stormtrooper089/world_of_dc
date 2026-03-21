package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.entity.VehicleDetails;
import org.dcoffice.cachar.service.VehicleExcelService;
import org.dcoffice.cachar.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleExcelService excelService;
    private final VehicleService vehicleService;

    public VehicleController(VehicleExcelService excelService, VehicleService vehicleService) {
        this.excelService = excelService;
        this.vehicleService = vehicleService;
    }

    // ─────────────────────────────────────────────────────────────
    // 📤 Upload Vehicle Excel
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<String> uploadVehicleExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try (InputStream inputStream = file.getInputStream()) {
            excelService.uploadExcel(inputStream);
            return ResponseEntity.ok("Vehicle Excel uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 Fetch vehicle details by psName or vehicleNo
    // GET /api/vehicles?psName=Polling Station A
    // GET /api/vehicles?vehicleNo=AS01AB1234
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<?>> fetchVehicles(
            @RequestParam(required = false) String psName,
            @RequestParam(required = false) String vehicleNo) {

        if (psName != null && !psName.isBlank()) {
            List<VehicleDetails> result = vehicleService.fetchByPsName(psName);
            return ResponseEntity.ok(ApiResponse.success("Vehicles fetched for psName: " + psName, result));
        }

        if (vehicleNo != null && !vehicleNo.isBlank()) {
            return vehicleService.fetchByVehicleNo(vehicleNo)
                    .<ResponseEntity<ApiResponse<?>>>map(v ->
                            ResponseEntity.ok(ApiResponse.success("Vehicle fetched", v)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Vehicle not found: " + vehicleNo)));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Provide at least one query param: psName or vehicleNo"));
    }

    // ─────────────────────────────────────────────────────────────
    // 📋 Fetch all vehicle numbers
    // GET /api/vehicles/all-vehicle-nos
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/all-vehicle-nos")
    public ResponseEntity<ApiResponse<List<String>>> fetchAllVehicleNos() {
        List<String> nos = vehicleService.fetchAllVehicleNos();
        return ResponseEntity.ok(ApiResponse.success("All vehicle numbers fetched", nos));
    }

    // ─────────────────────────────────────────────────────────────
    // ➕ Create vehicle
    // POST /api/vehicles
    // ─────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<VehicleDetails>> createVehicle(@RequestBody VehicleDetails vehicle) {
        try {
            VehicleDetails created = vehicleService.create(vehicle);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Vehicle created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create vehicle: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✏️ Update vehicle by psName or vehicleNo
    // PUT /api/vehicles?psName=Polling Station A
    // PUT /api/vehicles?vehicleNo=AS01AB1234
    // ─────────────────────────────────────────────────────────────
    @PutMapping
    public ResponseEntity<ApiResponse<?>> updateVehicle(
            @RequestParam(required = false) String psName,
            @RequestParam(required = false) String vehicleNo,
            @RequestBody VehicleDetails updated) {

        try {
            if (psName != null && !psName.isBlank()) {
                List<VehicleDetails> result = vehicleService.updateByPsName(psName, updated);
                return ResponseEntity.ok(ApiResponse.success("Vehicles updated for psName: " + psName, result));
            }
            if (vehicleNo != null && !vehicleNo.isBlank()) {
                VehicleDetails result = vehicleService.updateByVehicleNo(vehicleNo, updated);
                return ResponseEntity.ok(ApiResponse.success("Vehicle updated", result));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Provide at least one query param: psName or vehicleNo"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Update failed: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🗑️ Delete vehicle by psName or vehicleNo
    // DELETE /api/vehicles?psName=Polling Station A
    // DELETE /api/vehicles?vehicleNo=AS01AB1234
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @RequestParam(required = false) String psName,
            @RequestParam(required = false) String vehicleNo) {

        try {
            if (psName != null && !psName.isBlank()) {
                vehicleService.deleteByPsName(psName);
                return ResponseEntity.ok(ApiResponse.success("Vehicles deleted for psName: " + psName));
            }
            if (vehicleNo != null && !vehicleNo.isBlank()) {
                vehicleService.deleteByVehicleNo(vehicleNo);
                return ResponseEntity.ok(ApiResponse.success("Vehicle deleted: " + vehicleNo));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Provide at least one query param: psName or vehicleNo"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete failed: " + e.getMessage()));
        }
    }
}
