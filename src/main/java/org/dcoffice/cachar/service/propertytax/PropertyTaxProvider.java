package org.dcoffice.cachar.service.propertytax;

import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;

import java.util.List;
import java.util.Map;

public interface PropertyTaxProvider {
    String providerCode();
    Map<String, Object> getCitizenAccount(String citizenId);
    PropertyTaxAccount linkProperty(String citizenId, String holdingNumber);
    PropertyTaxPaymentReceipt payPropertyTax(String citizenId, String holdingNumber, String paymentMode);
    PropertyTaxPaymentReceipt verifyReceipt(String receiptNumber);
    Map<String, Object> dashboard();
    List<PropertyTaxAccount> defaulters();
}
