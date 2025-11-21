package org.dcoffice.cachar.dto;

/**
 * DTO for carousel slide content
 */
public class CarouselSlideDto {
    private String title;
    private String description;
    private String backgroundImage; // URL or path to image (optional)
    private String backgroundColor; // Simple color name (e.g., "blue", "green", "yellow") - frontend handles gradient styling

    public CarouselSlideDto() {
    }

    public CarouselSlideDto(String title, String description, String backgroundImage, String backgroundColor) {
        this.title = title;
        this.description = description;
        this.backgroundImage = backgroundImage;
        this.backgroundColor = backgroundColor;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}

