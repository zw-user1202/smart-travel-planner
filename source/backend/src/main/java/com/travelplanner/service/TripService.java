package com.travelplanner.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travelplanner.common.BizException;
import com.travelplanner.dto.PlanResult;
import com.travelplanner.dto.TripCreateRequest;
import com.travelplanner.dto.WorkbenchStats;
import com.travelplanner.entity.Trip;
import com.travelplanner.entity.User;
import com.travelplanner.mapper.TripMapper;
import com.travelplanner.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripMapper tripMapper;
    private final UserMapper userMapper;
    private final AiPlanService aiPlanService;

    public Trip create(Long userId, TripCreateRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BizException("返回日期不能早于出发日期");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
        if (days > 15) {
            throw new BizException("单次行程最长支持15天");
        }
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setDestination(req.getDestination());
        trip.setTitle(StringUtils.hasText(req.getTitle())
                ? req.getTitle()
                : req.getDestination() + " " + req.getStartDate() + " 行程");
        trip.setStartDate(req.getStartDate());
        trip.setEndDate(req.getEndDate());
        trip.setPeopleCount(req.getPeopleCount());
        trip.setBudget(req.getBudget());
        if (req.getPreferences() != null && !req.getPreferences().isEmpty()) {
            trip.setPreferences(String.join(",", req.getPreferences()));
        }
        trip.setStatus("draft");
        trip.setDeleted(0);
        tripMapper.insert(trip);
        return trip;
    }

    public PlanResult generatePlan(Long userId, Long tripId) {
        Trip trip = getOwned(userId, tripId);
        User user = userMapper.selectById(userId);
        if (user != null && user.getDailyQuota() != null && user.getDailyQuota() <= 0) {
            throw new BizException("今日AI规划额度已用完，明天再来或联系管理员提额");
        }
        PlanResult plan = aiPlanService.generate(trip);
        trip.setPlanJson(JSONUtil.toJsonStr(plan));
        trip.setAdvice(plan.getAdvice());
        trip.setEstimatedCost(plan.getEstimatedCost());
        trip.setAiProvider(plan.getProvider());
        trip.setAiModel(plan.getModel());
        trip.setStatus("generated");
        tripMapper.updateById(trip);
        if (user != null && user.getDailyQuota() != null) {
            user.setDailyQuota(user.getDailyQuota() - 1);
            userMapper.updateById(user);
        }
        return plan;
    }

    public PlanResult getPlan(Long userId, Long tripId) {
        Trip trip = getOwned(userId, tripId);
        if (!StringUtils.hasText(trip.getPlanJson())) {
            return null;
        }
        return JSONUtil.toBean(trip.getPlanJson(), PlanResult.class);
    }

    public Trip getOwned(Long userId, Long tripId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null || trip.getDeleted() != null && trip.getDeleted() == 1) {
            throw new BizException(404, "行程不存在");
        }
        if (!trip.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问该行程");
        }
        return trip;
    }

    public List<Trip> listMine(Long userId, String status) {
        LambdaQueryWrapper<Trip> qw = new LambdaQueryWrapper<Trip>()
                .eq(Trip::getUserId, userId)
                .orderByDesc(Trip::getUpdatedAt);
        if (StringUtils.hasText(status)) {
            qw.eq(Trip::getStatus, status);
        }
        return tripMapper.selectList(qw);
    }

    public Trip rename(Long userId, Long tripId, String title) {
        Trip trip = getOwned(userId, tripId);
        if (!StringUtils.hasText(title)) {
            throw new BizException("标题不能为空");
        }
        trip.setTitle(title);
        tripMapper.updateById(trip);
        return trip;
    }

    public void delete(Long userId, Long tripId) {
        Trip trip = getOwned(userId, tripId);
        trip.setDeleted(1);
        tripMapper.updateById(trip);
    }

    public Trip updateStatus(Long userId, Long tripId, String status) {
        Trip trip = getOwned(userId, tripId);
        List<String> allowed = List.of("draft", "generated", "ongoing", "done");
        if (!allowed.contains(status)) {
            throw new BizException("非法状态");
        }
        trip.setStatus(status);
        tripMapper.updateById(trip);
        return trip;
    }

    public WorkbenchStats stats(Long userId) {
        WorkbenchStats s = new WorkbenchStats();
        LambdaQueryWrapper<Trip> base = new LambdaQueryWrapper<Trip>().eq(Trip::getUserId, userId);
        s.setTotal(tripMapper.selectCount(base));
        s.setPlanned(tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getUserId, userId).eq(Trip::getStatus, "draft")));
        s.setGenerated(tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getUserId, userId).eq(Trip::getStatus, "generated")));
        s.setOngoing(tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getUserId, userId).eq(Trip::getStatus, "ongoing")));
        s.setDone(tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getUserId, userId).eq(Trip::getStatus, "done")));

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalEst = BigDecimal.ZERO;
        for (Trip t : tripMapper.selectList(base)) {
            if (t.getBudget() != null) {
                totalBudget = totalBudget.add(t.getBudget());
            }
            if (t.getEstimatedCost() != null) {
                totalEst = totalEst.add(t.getEstimatedCost());
            }
        }
        s.setTotalBudget(totalBudget);
        s.setTotalEstimated(totalEst);

        User user = userMapper.selectById(userId);
        if (user != null) {
            s.setDailyQuota(user.getDailyQuota() == null ? 5 : user.getDailyQuota());
            s.setTodayUsedQuota(Math.max(0, 5 - s.getDailyQuota()));
        }
        List<Trip> recent = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getUserId, userId)
                .orderByDesc(Trip::getUpdatedAt)
                .last("limit 5"));
        s.setRecentTrips(recent.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("destination", t.getDestination());
            m.put("status", t.getStatus());
            m.put("startDate", t.getStartDate());
            m.put("endDate", t.getEndDate());
            m.put("updatedAt", t.getUpdatedAt());
            return m;
        }).toList());
        return s;
    }
}
