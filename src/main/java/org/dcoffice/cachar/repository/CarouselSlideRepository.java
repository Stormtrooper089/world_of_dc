package org.dcoffice.cachar.repository;

import org.dcoffice.cachar.entity.CarouselSlide;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarouselSlideRepository extends MongoRepository<CarouselSlide, String> {

    /**
     * Find all active carousel slides ordered by display order
     */
    @Query("{ 'isActive': true }")
    List<CarouselSlide> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find all carousel slides (including inactive) ordered by display order
     */
    List<CarouselSlide> findAllByOrderByDisplayOrderAsc();

    /**
     * Find carousel slide by ID
     */
    Optional<CarouselSlide> findById(String id);

    /**
     * Check if a slide exists with the given display order
     */
    boolean existsByDisplayOrder(int displayOrder);
}

