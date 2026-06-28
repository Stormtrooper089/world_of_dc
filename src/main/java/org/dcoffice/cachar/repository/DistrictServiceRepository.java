package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.DistrictService;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictServiceRepository extends MongoRepository<DistrictService, String> {
    List<DistrictService> findByActiveTrueOrderByDisplayOrderAscServiceNameAsc();
    Optional<DistrictService> findByServiceCode(String serviceCode);
    long countByActiveTrue();
}
