package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.PollingStation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PollingStationRepository extends MongoRepository<PollingStation, String> {
    List<PollingStation> findByLacNo(int lacNo);
    List<PollingStation> findByH3Index(String h3Index);
}