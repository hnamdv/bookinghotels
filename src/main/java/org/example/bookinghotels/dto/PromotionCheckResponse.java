package org.example.bookinghotels.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PromotionCheckResponse {

    private boolean valid;
    private String message;
    private Integer promotionId;
    private String promotionName;
    private Double discountPercent;
}