package com.travelplanner.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TripCreateRequest {
    @NotBlank(message = "目的地不能为空")
    private String destination;

    @NotNull(message = "出发日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "返回日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "出行人数不能为空")
    @Positive(message = "出行人数必须大于0")
    private Integer peopleCount;

    private BigDecimal budget;

    private String title;

    private List<String> preferences;
}
