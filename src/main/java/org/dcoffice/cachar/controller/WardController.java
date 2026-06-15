package org.dcoffice.cachar.controller;

import org.dcoffice.cachar.dto.ApiResponse;
import org.dcoffice.cachar.entity.Ward;
import org.dcoffice.cachar.service.WardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wards")
@CrossOrigin(origins = "*")
public class WardController {

    private final WardService wardService;

    public WardController(WardService wardService) {
        this.wardService = wardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Ward>>> getActiveWards() {
        return ResponseEntity.ok(ApiResponse.success("Wards retrieved successfully", wardService.getActiveWards()));
    }

    @GetMapping("/{wardNumber}")
    public ResponseEntity<ApiResponse<Ward>> getWard(@PathVariable Integer wardNumber) {
        return wardService.findByWardNumber(wardNumber)
                .map(ward -> ResponseEntity.ok(ApiResponse.success("Ward retrieved successfully", ward)))
                .orElseGet(() -> ResponseEntity.<ApiResponse<Ward>>notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Ward>> upsertWard(@RequestBody Ward ward) {
        if (ward.getWardNumber() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ward number is required"));
        }
        Ward saved = wardService.save(ward);
        return ResponseEntity.ok(ApiResponse.success("Ward saved successfully", saved));
    }
}
