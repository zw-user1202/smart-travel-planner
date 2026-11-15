package com.travelplanner.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplanner.common.BizException;
import com.travelplanner.common.Result;
import com.travelplanner.entity.SysConfig;
import com.travelplanner.entity.User;
import com.travelplanner.mapper.SysConfigMapper;
import com.travelplanner.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;

    private void checkAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User u = userMapper.selectById(userId);
        if (u == null || u.getRole() == null || u.getRole() != 1) {
            throw new BizException(403, "无管理员权限");
        }
    }

    @GetMapping("/users")
    public Result<List<User>> listUsers(HttpServletRequest request) {
        checkAdmin(request);
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getDeleted, 0).orderByDesc(User::getId));
        return Result.ok(users);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        checkAdmin(request);
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException("用户不存在");
        u.setStatus(body.get("status"));
        userMapper.updateById(u);
        return Result.ok();
    }

    @PutMapping("/users/{id}/quota")
    public Result<Void> updateUserQuota(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        checkAdmin(request);
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException("用户不存在");
        u.setDailyQuota(body.get("dailyQuota"));
        userMapper.updateById(u);
        return Result.ok();
    }

    @GetMapping("/config")
    public Result<List<SysConfig>> getConfig(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(sysConfigMapper.selectList(null));
    }

    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody List<Map<String, String>> items, HttpServletRequest request) {
        checkAdmin(request);
        for (Map<String, String> item : items) {
            String key = item.get("cfgKey");
            String value = item.get("cfgValue");
            SysConfig existing = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getCfgKey, key));
            if (existing != null) {
                existing.setCfgValue(value);
                sysConfigMapper.updateById(existing);
            } else {
                SysConfig cfg = new SysConfig();
                cfg.setCfgKey(key);
                cfg.setCfgValue(value);
                cfg.setRemark(item.get("remark"));
                sysConfigMapper.insert(cfg);
            }
        }
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> adminStats(HttpServletRequest request) {
        checkAdmin(request);
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        Long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0).eq(User::getStatus, 1));
        Long adminUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0).eq(User::getRole, 1));
        return Result.ok(Map.of("totalUsers", totalUsers, "activeUsers", activeUsers, "adminUsers", adminUsers));
    }
}
