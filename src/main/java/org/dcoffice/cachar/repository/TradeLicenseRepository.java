package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.TradeLicense;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TradeLicenseRepository extends MongoRepository<TradeLicense, String> {
    Optional<TradeLicense> findByLicenseNumber(String licenseNumber);
    List<TradeLicense> findByLinkedCitizenId(String linkedCitizenId);
    List<TradeLicense> findByStatus(String status);
    long countByStatus(String status);
}
