package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.VehicleDetails;
import org.dcoffice.cachar.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    public List<VehicleDetails> fetchByPsName(String psName) {
        return repository.findByPsName(psName);
    }

    public Optional<VehicleDetails> fetchByVehicleNo(String vehicleNo) {
        return repository.findByVehicleNo(vehicleNo);
    }

    public List<String> fetchAllVehicleNos() {
        return repository.findAllVehicleNos().stream()
                .map(VehicleDetails::getVehicleNo)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toList());
    }

    // Returns list of { acNo, vehicleNo } mappings for UI options
    public List<java.util.Map<String, String>> fetchAcNoMappings() {
        return repository.findAllAcNoAndVehicleNo().stream()
                .filter(v -> v.getAcNo() != null && !v.getAcNo().isBlank())
                .map(v -> {
                    java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
                    map.put("acNo", v.getAcNo());
                    map.put("vehicleNo", v.getVehicleNo());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<VehicleDetails> fetchLocationByAcNo(String acNo) {
        List<VehicleDetails> vehicles = repository.findByAcNo(acNo);
        if (vehicles.isEmpty()) {
            throw new RuntimeException("Vehicle not found for acNo: " + acNo);
        }
        return vehicles;
    }

    public List<VehicleDetails> updateLocationByAcNo(String acNo, VehicleDetails src) {
        List<VehicleDetails> existingVehicles = repository.findByAcNo(acNo);
        if (existingVehicles.isEmpty()) {
            throw new RuntimeException("Vehicle not found for acNo: " + acNo);
        }

        existingVehicles.forEach(existing -> {
            boolean locationUpdated = false;
            if (src.getLocation() != null) {
                existing.setLocation(src.getLocation());
                locationUpdated = true;
            }
            if (src.getRemarks() != null) {
                existing.setRemarks(src.getRemarks());
            }
            if (src.getParkingAddress() != null) {
                existing.setParkingAddress(src.getParkingAddress());
                locationUpdated = true;
            }
            if (src.getStatusComment() != null) {
                existing.setStatusComment(src.getStatusComment());
            }
            // Auto-update lastLocationUpdate timestamp when location or parkingAddress changes
            if (locationUpdated) {
                existing.setLastLocationUpdate(System.currentTimeMillis());
            }
        });

        return repository.saveAll(existingVehicles);
    }

    public VehicleDetails create(VehicleDetails vehicle) {
        vehicle.setId(null); // ensure Mongo generates an ID
        vehicle.setUploadTime(System.currentTimeMillis());
        return repository.save(vehicle);
    }

    public List<VehicleDetails> updateByPsName(String psName, VehicleDetails updated) {
        List<VehicleDetails> existing = repository.findByPsName(psName);
        if (existing.isEmpty()) {
            throw new RuntimeException("No vehicles found for psName: " + psName);
        }
        existing.forEach(v -> applyUpdates(v, updated));
        return repository.saveAll(existing);
    }

    public VehicleDetails updateByVehicleNo(String vehicleNo, VehicleDetails updated) {
        VehicleDetails existing = repository.findByVehicleNo(vehicleNo)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleNo));
        applyUpdates(existing, updated);
        return repository.save(existing);
    }

    @Transactional
    public void deleteByPsName(String psName) {
        List<VehicleDetails> existing = repository.findByPsName(psName);
        if (existing.isEmpty()) {
            throw new RuntimeException("No vehicles found for psName: " + psName);
        }
        repository.deleteByPsName(psName);
    }

    @Transactional
    public void deleteByVehicleNo(String vehicleNo) {
        repository.findByVehicleNo(vehicleNo)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleNo));
        repository.deleteByVehicleNo(vehicleNo);
    }

    private void applyUpdates(VehicleDetails target, VehicleDetails src) {
        if (src.getAcNo() != null)        target.setAcNo(src.getAcNo());
        if (src.getPsNo() != null)        target.setPsNo(src.getPsNo());
        if (src.getPsName() != null)      target.setPsName(src.getPsName());
        if (src.getVehicleNo() != null)   target.setVehicleNo(src.getVehicleNo());
        if (src.getDriverName() != null)  target.setDriverName(src.getDriverName());
        if (src.getDriverMobile() != null) target.setDriverMobile(src.getDriverMobile());
        if (src.getVehicleType() != null) target.setVehicleType(src.getVehicleType());
        if (src.getCapacity() != null)    target.setCapacity(src.getCapacity());
        if (src.getRoute() != null)       target.setRoute(src.getRoute());
        if (src.getRemarks() != null)     target.setRemarks(src.getRemarks());
        if (src.getLocation() != null) {
            target.setLocation(src.getLocation());
            target.setLastLocationUpdate(System.currentTimeMillis());
        }
        if (src.getParkingAddress() != null) {
            target.setParkingAddress(src.getParkingAddress());
            if (src.getLocation() == null) {
                target.setLastLocationUpdate(System.currentTimeMillis());
            }
        }
        if (src.getStatusComment() != null)  target.setStatusComment(src.getStatusComment());
        if (src.getLastLocationUpdate() != null) target.setLastLocationUpdate(src.getLastLocationUpdate());
    }
}
