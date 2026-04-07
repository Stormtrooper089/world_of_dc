package org.dcoffice.cachar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.entity.VehicleDetails;
import org.dcoffice.cachar.service.VehicleExcelService;
import org.dcoffice.cachar.service.VehicleService;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
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
    // 🆔 Fetch acNo → vehicleNo mappings (UI options)
    // GET /api/vehicles/vehicle-id-mappings
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/vehicle-id-mappings")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, String>>>> fetchVehicleIdMappings() {
        List<java.util.Map<String, String>> mappings = vehicleService.fetchAcNoMappings();
        return ResponseEntity.ok(ApiResponse.success("Vehicle ID mappings fetched", mappings));
    }

    // ─────────────────────────────────────────────────────────────
    // 📍 Fetch location details by acNo
    // GET /api/vehicles/location?acNo=101
    // Returns: full vehicle record with location, parkingAddress, statusComment, lastLocationUpdate
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/location")
    public ResponseEntity<ApiResponse<List<VehicleDetails>>> fetchLocation(
            @RequestParam String acNo) {
        try {
            List<VehicleDetails> vehicles = vehicleService.fetchLocationByAcNo(acNo);
            return ResponseEntity.ok(ApiResponse.success("Location fetched", vehicles));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch location: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ✏️ Update location details by acNo
    // PUT /api/vehicles/location?acNo=101
    // Body: { "location": {...}, "parkingAddress": "...", "statusComment": "..." }
    // Note: lastLocationUpdate is automatically set when location or parkingAddress changes
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/location")
    public ResponseEntity<ApiResponse<List<VehicleDetails>>> updateLocation(
            @RequestParam String acNo,
            @RequestBody JsonNode payload) {
        try {
            VehicleDetails updated = new VehicleDetails();

            JsonNode locationNode = payload.get("location");
            if (locationNode != null && !locationNode.isNull()) {
                GeoJsonPoint parsedLocation = parseLocation(locationNode);
                if (parsedLocation == null) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid location format. Use {\"location\":{\"x\":<lng>,\"y\":<lat>}} or GeoJSON coordinates."));
                }
                updated.setLocation(parsedLocation);
            }

            JsonNode remarksNode = payload.get("remarks");
            if (remarksNode != null && !remarksNode.isNull()) {
                updated.setRemarks(remarksNode.asText());
            }

            JsonNode parkingAddressNode = payload.get("parkingAddress");
            if (parkingAddressNode != null && !parkingAddressNode.isNull()) {
                updated.setParkingAddress(parkingAddressNode.asText());
            }

            JsonNode statusCommentNode = payload.get("statusComment");
            if (statusCommentNode != null && !statusCommentNode.isNull()) {
                updated.setStatusComment(statusCommentNode.asText());
            }

            List<VehicleDetails> result = vehicleService.updateLocationByAcNo(acNo, updated);
            return ResponseEntity.ok(ApiResponse.success("Location updated", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update location: " + e.getMessage()));
        }
    }

    private GeoJsonPoint parseLocation(JsonNode locationNode) {
        // Supports {"x": <longitude>, "y": <latitude>}
        if (locationNode.hasNonNull("x") && locationNode.hasNonNull("y")) {
            return new GeoJsonPoint(locationNode.get("x").asDouble(), locationNode.get("y").asDouble());
        }

        // Supports {"longitude": <longitude>, "latitude": <latitude>}
        if (locationNode.hasNonNull("longitude") && locationNode.hasNonNull("latitude")) {
            return new GeoJsonPoint(locationNode.get("longitude").asDouble(), locationNode.get("latitude").asDouble());
        }

        // Supports GeoJSON-like payloads with coordinates array: [longitude, latitude]
        JsonNode coordinates = locationNode.get("coordinates");
        if (coordinates != null && coordinates.isArray() && coordinates.size() >= 2
                && coordinates.get(0).isNumber() && coordinates.get(1).isNumber()) {
            return new GeoJsonPoint(coordinates.get(0).asDouble(), coordinates.get(1).asDouble());
        }

        return null;
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
