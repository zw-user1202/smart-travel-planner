package com.travelplanner.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.travelplanner.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德开放平台 Web 服务：地理编码 / POI / 天气。
 * 未配置 Key 时静默返回 null，不影响主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapService {

    private final AppProperties props;

    /** 搜索目的地 POI，返回 lnglat=[lng,lat], name, image 可能为 null */
    public Map<String, double[]> searchPoi(String city) {
        if (!StringUtils.hasText(props.getAmap().getKey()) || !StringUtils.hasText(city)) {
            return null;
        }
        try {
            String url = props.getAmap().getBaseUrl() + "/v3/place/text?" +
                    "key=" + props.getAmap().getKey() +
                    "&keywords=" + URLEncoder.encode(city, StandardCharsets.UTF_8) +
                    "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8) +
                    "&offset=1&page=1&extensions=all";
            String body = HttpUtil.get(url);
            JSONObject obj = JSONUtil.parseObj(body);
            if (!"1".equals(obj.getStr("status"))) {
                return null;
            }
            Object pois = obj.get("pois");
            if (pois == null) {
                return null;
            }
            cn.hutool.json.JSONArray arr = JSONUtil.parseArray(pois.toString());
            if (arr.isEmpty()) {
                return null;
            }
            JSONObject first = arr.getJSONObject(0);
            String location = first.getStr("location");
            if (!StringUtils.hasText(location)) {
                return null;
            }
            String[] parts = location.split(",");
            Map<String, double[]> map = new HashMap<>();
            map.put("lnglat", new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
            return map;
        } catch (Exception e) {
            log.warn("高德POI查询失败: {}", e.getMessage());
            return null;
        }
    }

    /** 地理编码：地址 → 经纬度 */
    public double[] geocode(String address) {
        if (!StringUtils.hasText(props.getAmap().getKey()) || !StringUtils.hasText(address)) {
            return null;
        }
        try {
            String url = props.getAmap().getBaseUrl() + "/v3/geocode/geo?" +
                    "key=" + props.getAmap().getKey() +
                    "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
            JSONObject obj = JSONUtil.parseObj(HttpUtil.get(url));
            if (!"1".equals(obj.getStr("status"))) {
                return null;
            }
            cn.hutool.json.JSONArray arr = obj.getJSONArray("geocodes");
            if (arr == null || arr.isEmpty()) {
                return null;
            }
            String loc = arr.getJSONObject(0).getStr("location");
            String[] p = loc.split(",");
            return new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1])};
        } catch (Exception e) {
            log.warn("高德地理编码失败: {}", e.getMessage());
            return null;
        }
    }

    /** 实时天气 */
    public Map<String, Object> weatherLive(String city) {
        return fetchWeather(city, "base");
    }

    /** 预报天气 */
    public Map<String, Object> weatherForecast(String city) {
        return fetchWeather(city, "all");
    }

    private Map<String, Object> fetchWeather(String city, String extensions) {
        Map<String, Object> out = new HashMap<>();
        if (!StringUtils.hasText(props.getAmap().getKey()) || !StringUtils.hasText(city)) {
            return out;
        }
        try {
            String url = props.getAmap().getBaseUrl() + "/v3/weather/weatherInfo?" +
                    "key=" + props.getAmap().getKey() +
                    "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8) +
                    "&extensions=" + extensions;
            JSONObject obj = JSONUtil.parseObj(HttpUtil.get(url));
            if (!"1".equals(obj.getStr("status"))) {
                return out;
            }
            if ("base".equals(extensions)) {
                cn.hutool.json.JSONArray lives = obj.getJSONArray("lives");
                if (lives != null && !lives.isEmpty()) {
                    out.put("live", lives.getJSONObject(0));
                }
            } else {
                cn.hutool.json.JSONArray forecasts = obj.getJSONArray("forecasts");
                if (forecasts != null && !forecasts.isEmpty()) {
                    out.put("forecast", forecasts.getJSONObject(0));
                }
            }
        } catch (Exception e) {
            log.warn("高德天气查询失败: {}", e.getMessage());
        }
        return out;
    }
}
