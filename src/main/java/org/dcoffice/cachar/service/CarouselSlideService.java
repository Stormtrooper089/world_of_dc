package org.dcoffice.cachar.service;

import org.dcoffice.cachar.dto.CarouselSlideDto;
import org.dcoffice.cachar.entity.CarouselSlide;
import org.dcoffice.cachar.repository.CarouselSlideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CarouselSlideService {

    private static final Logger logger = LoggerFactory.getLogger(CarouselSlideService.class);

    @Autowired
    private CarouselSlideRepository carouselSlideRepository;

    /**
     * Get all active carousel slides ordered by display order
     */
    public List<CarouselSlideDto> getActiveCarouselSlides() {
        List<CarouselSlide> slides = carouselSlideRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return slides.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all carousel slides (including inactive) ordered by display order
     */
    public List<CarouselSlide> getAllCarouselSlides() {
        return carouselSlideRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Get carousel slide by ID
     */
    public Optional<CarouselSlide> getCarouselSlideById(String id) {
        return carouselSlideRepository.findById(id);
    }

    /**
     * Create a new carousel slide
     */
    public CarouselSlide createCarouselSlide(CarouselSlide slide) {
        slide.setCreatedAt(LocalDateTime.now());
        slide.setUpdatedAt(LocalDateTime.now());
        if (slide.getDisplayOrder() == 0) {
            // Auto-assign display order if not provided
            List<CarouselSlide> allSlides = carouselSlideRepository.findAllByOrderByDisplayOrderAsc();
            int maxOrder = allSlides.stream()
                    .mapToInt(CarouselSlide::getDisplayOrder)
                    .max()
                    .orElse(0);
            slide.setDisplayOrder(maxOrder + 1);
        }
        logger.info("Created carousel slide: {}", slide.getTitle());
        return carouselSlideRepository.save(slide);
    }

    /**
     * Update an existing carousel slide
     */
    public CarouselSlide updateCarouselSlide(String id, CarouselSlide updatedSlide) {
        Optional<CarouselSlide> existingOpt = carouselSlideRepository.findById(id);
        if (!existingOpt.isPresent()) {
            throw new IllegalArgumentException("Carousel slide not found with ID: " + id);
        }

        CarouselSlide existing = existingOpt.get();
        existing.setTitle(updatedSlide.getTitle());
        existing.setDescription(updatedSlide.getDescription());
        existing.setBackgroundImage(updatedSlide.getBackgroundImage());
        existing.setBackgroundColor(updatedSlide.getBackgroundColor());
        existing.setDisplayOrder(updatedSlide.getDisplayOrder());
        existing.setActive(updatedSlide.isActive());
        existing.setUpdatedAt(LocalDateTime.now());

        logger.info("Updated carousel slide: {}", existing.getTitle());
        return carouselSlideRepository.save(existing);
    }

    /**
     * Delete a carousel slide
     */
    public void deleteCarouselSlide(String id) {
        if (!carouselSlideRepository.existsById(id)) {
            throw new IllegalArgumentException("Carousel slide not found with ID: " + id);
        }
        carouselSlideRepository.deleteById(id);
        logger.info("Deleted carousel slide with ID: {}", id);
    }

    /**
     * Convert entity to DTO
     */
    private CarouselSlideDto convertToDto(CarouselSlide slide) {
        CarouselSlideDto dto = new CarouselSlideDto();
        dto.setTitle(slide.getTitle());
        dto.setDescription(slide.getDescription());
        dto.setBackgroundImage(slide.getBackgroundImage());
        dto.setBackgroundColor(slide.getBackgroundColor());
        return dto;
    }
}

