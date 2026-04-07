package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.PollingParty;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PollingPartyRepository extends MongoRepository<PollingParty, String> {

    // ✅ NOW ONLY PS-BASED
    Optional<PollingParty> findByPsNo(String psNo);
   java.util.List<PollingParty> findByPsName(String psName);
   Optional<PollingParty> findByPsNameIgnoreCase(String psName);
}