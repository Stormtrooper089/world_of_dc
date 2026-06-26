package org.dcoffice.cachar.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class TradeLicenseFeedbackRequest {
    @Min(1)
    @Max(5)
    private Integer rating;
    private String feedback;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
