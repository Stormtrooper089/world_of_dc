package org.dcoffice.cachar.service.propertytax;

import org.dcoffice.cachar.entity.PropertyTaxAccount;
import org.dcoffice.cachar.entity.PropertyTaxPaymentReceipt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UpyogPropertyTaxProvider implements PropertyTaxProvider {
    @Override
    public String providerCode() {
        return "UPYOG";
    }

    @Override
    public Map<String, Object> getCitizenAccount(String citizenId) {
        throw unavailable();
    }

    @Override
    public PropertyTaxAccount linkProperty(String citizenId, String holdingNumber) {
        throw unavailable();
    }

    @Override
    public PropertyTaxPaymentReceipt payPropertyTax(String citizenId, String holdingNumber, String paymentMode) {
        throw unavailable();
    }

    @Override
    public PropertyTaxPaymentReceipt verifyReceipt(String receiptNumber) {
        throw unavailable();
    }

    @Override
    public Map<String, Object> dashboard() {
        throw unavailable();
    }

    @Override
    public List<PropertyTaxAccount> defaulters() {
        throw unavailable();
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException("UPYOG provider is configured but API credentials/mapping are not connected yet");
    }
}
