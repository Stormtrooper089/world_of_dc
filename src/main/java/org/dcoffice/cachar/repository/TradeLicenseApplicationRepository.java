package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.TradeLicenseApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TradeLicenseApplicationRepository extends MongoRepository<TradeLicenseApplication, String> {
    List<TradeLicenseApplication> findByCitizenIdOrderBySubmittedAtDesc(String citizenId);
    Optional<TradeLicenseApplication> findByApplicationNumberIgnoreCase(String applicationNumber);
    long countByStatus(String status);
}
