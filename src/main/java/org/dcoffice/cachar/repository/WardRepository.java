package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.Ward;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends MongoRepository<Ward, String> {
    Optional<Ward> findByWardNumber(Integer wardNumber);
    List<Ward> findByActiveTrueOrderByWardNumberAsc();
    List<Ward> findByZoneAndActiveTrueOrderByWardNumberAsc(String zone);
}
