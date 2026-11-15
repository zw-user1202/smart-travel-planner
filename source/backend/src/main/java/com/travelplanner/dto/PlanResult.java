package com.travelplanner.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能规划结果（DeepSeek 返回 / 本地兜底共用）
 */
@Data
public class PlanResult {
    private String summary;
    private BigDecimal estimatedCost;
    private String advice;
    private List<DayPlan> days;
    private String provider;
    private String model;

    @Data
    public static class DayPlan {
        private int day;
        private String date;
        private String theme;
        private List<ScheduleItem> items;
    }

    @Data
    public static class ScheduleItem {
        /** morning / noon / afternoon / evening / night */
        private String slot;
        private String type;
        private String name;
        private String description;
        private Double lng;
        private Double lat;
        private BigDecimal cost;
        private String image;
    }
}
