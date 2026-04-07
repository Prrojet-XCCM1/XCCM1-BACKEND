package com.ihm.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDistributionDTO {
    
    private long excellent;  // 90-100%
    private long good;       // 70-89%
    private long average;    // 50-69%
    private long poor;       // <50%
    
    public long getTotal() {
        return excellent + good + average + poor;
    }
}
