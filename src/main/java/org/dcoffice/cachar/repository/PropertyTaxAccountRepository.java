package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyTaxAccountRepository extends MongoRepository<PropertyTaxAccount, String> {
    Optional<PropertyTaxAccount> findByHoldingNumber(String holdingNumber);
    List<PropertyTaxAccount> findByLinkedCitizenId(String linkedCitizenId);
    List<PropertyTaxAccount> findByWardNumber(Integer wardNumber);
    List<PropertyTaxAccount> findByStatus(String status);
    long countByStatus(String status);
}
