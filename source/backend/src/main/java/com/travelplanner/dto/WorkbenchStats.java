package com.travelplanner.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class WorkbenchStats {
    private long total;
    private long planned;
    private long generated;
    private long ongoing;
    private long done;
    private BigDecimal totalBudget;
    private BigDecimal totalEstimated;
    private int todayUsedQuota;
    private int dailyQuota;
    private List<Map<String, Object>> recentTrips;
}
