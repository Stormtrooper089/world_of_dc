package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.DistanceMatrixCache;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DistanceMatrixCacheRepository
        extends MongoRepository<DistanceMatrixCache, String> {

    Optional<DistanceMatrixCache> findByCacheKey(String cacheKey);
}
