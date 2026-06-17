package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.PropertyTaxServiceRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyTaxServiceRequestRepository extends MongoRepository<PropertyTaxServiceRequest, String> {
    List<PropertyTaxServiceRequest> findByCitizenIdOrderBySubmittedAtDesc(String citizenId);
}
