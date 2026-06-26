package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.TradeLicenseApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TradeLicenseApplicationRepository extends MongoRepository<TradeLicenseApplication, String> {
    List<TradeLicenseApplication> findByCitizenIdOrderBySubmittedAtDesc(String citizenId);
    long countByStatus(String status);
}
