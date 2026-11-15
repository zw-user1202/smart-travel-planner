package com.travelplanner.controller;

import com.travelplanner.common.Result;
import com.travelplanner.dto.WorkbenchStats;
import com.travelplanner.service.AmapService;
import com.travelplanner.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkbenchController {

    private final TripService tripService;
    private final AmapService amapService;

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }

    @GetMapping("/workbench/stats")
    public Result<WorkbenchStats> stats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.stats(userId));
    }

    @GetMapping("/map/weather")
    public Result<Map<String, Object>> weather(@RequestParam String city) {
        Map<String, Object> map = amapService.weatherLive(city);
        Map<String, Object> forecast = amapService.weatherForecast(city);
        map.putAll(forecast);
        return Result.ok(map);
    }

    @GetMapping("/map/geocode")
    public Result<double[]> geocode(@RequestParam String address) {
        return Result.ok(amapService.geocode(address));
    }
}
