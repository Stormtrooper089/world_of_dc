package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Ward;
import org.dcoffice.cachar.repository.WardRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WardService {

    private final WardRepository wardRepository;

    public WardService(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
    }

    public List<Ward> getActiveWards() {
        return wardRepository.findByActiveTrueOrderByWardNumberAsc();
    }

    public Optional<Ward> findByWardNumber(Integer wardNumber) {
        return wardRepository.findByWardNumber(wardNumber);
    }

    public Ward save(Ward ward) {
        Instant now = Instant.now();
        if (ward.getCreatedAt() == null) {
            ward.setCreatedAt(now);
        }
        ward.setUpdatedAt(now);
        return wardRepository.save(ward);
    }
}
