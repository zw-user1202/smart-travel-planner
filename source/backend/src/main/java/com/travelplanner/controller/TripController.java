package com.travelplanner.controller;

import com.travelplanner.common.Result;
import com.travelplanner.dto.PlanResult;
import com.travelplanner.dto.TripCreateRequest;
import com.travelplanner.entity.Trip;
import com.travelplanner.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public Result<List<Trip>> list(@RequestParam(required = false) String status, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.listMine(userId, status));
    }

    @PostMapping
    public Result<Trip> create(@Valid @RequestBody TripCreateRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.create(userId, req));
    }

    @GetMapping("/{id}")
    public Result<Trip> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.getOwned(userId, id));
    }

    @GetMapping("/{id}/plan")
    public Result<PlanResult> plan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.getPlan(userId, id));
    }

    @PostMapping("/{id}/generate")
    public Result<PlanResult> generate(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.generatePlan(userId, id));
    }

    @PutMapping("/{id}/title")
    public Result<Trip> rename(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.rename(userId, id, body.get("title")));
    }

    @PutMapping("/{id}/status")
    public Result<Trip> status(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(tripService.updateStatus(userId, id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        tripService.delete(userId, id);
        return Result.ok();
    }
}
