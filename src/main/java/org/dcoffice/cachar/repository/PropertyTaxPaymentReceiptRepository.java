package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyTaxPaymentReceiptRepository extends MongoRepository<PropertyTaxPaymentReceipt, String> {
    List<PropertyTaxPaymentReceipt> findByCitizenIdOrderByPaidAtDesc(String citizenId);
    List<PropertyTaxPaymentReceipt> findByHoldingNumberOrderByPaidAtDesc(String holdingNumber);
}
