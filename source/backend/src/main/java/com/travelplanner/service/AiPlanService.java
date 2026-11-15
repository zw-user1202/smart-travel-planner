package com.travelplanner.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.travelplanner.config.AppProperties;
import com.travelplanner.dto.PlanResult;
import com.travelplanner.entity.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能规划服务：优先调用 DeepSeek 大模型；无 Key 或失败时使用本地规则兜底，保证演示可用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPlanService {

    private static final String JSON_SCHEMA_HINT =
            "{\"summary\":\"一句话总览\",\"estimatedCost\":数字,\"advice\":\"总览建议\",\"days\":[{\"day\":1,\"date\":\"yyyy-MM-dd\",\"theme\":\"主题\",\"items\":[{\"slot\":\"morning|noon|afternoon|evening|night\",\"type\":\"景点|餐饮|住宿|交通\",\"name\":\"名称\",\"description\":\"说明\",\"lng\":0.0,\"lat\":0.0,\"cost\":0}]}]}";

    private final AppProperties props;
    private final AmapService amapService;

    public PlanResult generate(Trip trip) {
        PlanResult deepseek = tryDeepSeek(trip);
        if (deepseek != null && deepseek.getDays() != null && !deepseek.getDays().isEmpty()) {
            deepseek.setProvider("DeepSeek");
            deepseek.setModel(props.getDeepseek().getModel());
            return deepseek;
        }
        PlanResult local = localPlan(trip);
        local.setProvider("本地规则引擎");
        local.setModel("rule-based-fallback");
        return local;
    }

    private PlanResult tryDeepSeek(Trip trip) {
        String apiKey = props.getDeepseek().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.info("DeepSeek API Key 未配置，使用本地规划兜底");
            return null;
        }
        try {
            long days = java.time.temporal.ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
            String userPrompt = buildPrompt(trip, days);
            JSONObject body = new JSONObject()
                    .set("model", props.getDeepseek().getModel())
                    .set("temperature", 0.7)
                    .set("messages", List.of(
                            Map.of("role", "system", "content",
                                    "你是专业旅行规划师。只输出 JSON，不要输出其它文字。格式：" + JSON_SCHEMA_HINT),
                            Map.of("role", "user", "content", userPrompt)
                    ));

            String url = props.getDeepseek().getBaseUrl() + "/chat/completions";
            String resp = cn.hutool.http.HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute()
                    .body();

            JSONObject root = JSONUtil.parseObj(resp);
            String content = root.getByPath("choices[0].message.content", String.class);
            if (!StringUtils.hasText(content)) {
                return null;
            }
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```(json)?\\s*", "").replaceAll("```\\s*$", "");
            }
            JSONObject plan = JSONUtil.parseObj(content);
            PlanResult result = new PlanResult();
            result.setSummary(plan.getStr("summary", "已为你生成行程方案"));
            result.setAdvice(plan.getStr("advice", ""));
            Object cost = plan.get("estimatedCost");
            if (cost != null) {
                result.setEstimatedCost(new BigDecimal(String.valueOf(cost)));
            }
            List<PlanResult.DayPlan> dayList = new ArrayList<>();
            for (Object o : plan.getJSONArray("days")) {
                JSONObject d = (JSONObject) o;
                PlanResult.DayPlan day = new PlanResult.DayPlan();
                day.setDay(d.getInt("day", 1));
                day.setDate(d.getStr("date", ""));
                day.setTheme(d.getStr("theme", ""));
                List<PlanResult.ScheduleItem> items = new ArrayList<>();
                for (Object io : d.getJSONArray("items")) {
                    JSONObject item = (JSONObject) io;
                    PlanResult.ScheduleItem si = new PlanResult.ScheduleItem();
                    si.setSlot(item.getStr("slot", "morning"));
                    si.setType(item.getStr("type", "景点"));
                    si.setName(item.getStr("name", ""));
                    si.setDescription(item.getStr("description", ""));
                    si.setLng(item.getDouble("lng"));
                    si.setLat(item.getDouble("lat"));
                    Object c = item.get("cost");
                    if (c != null) {
                        si.setCost(new BigDecimal(String.valueOf(c)));
                    }
                    items.add(si);
                }
                day.setItems(items);
                dayList.add(day);
            }
            result.setDays(dayList);
            return result;
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败，使用本地规划兜底: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(Trip trip, long days) {
        StringBuilder sb = new StringBuilder();
        sb.append("目的地：").append(trip.getDestination()).append("\n");
        sb.append("日期：").append(trip.getStartDate()).append(" 至 ").append(trip.getEndDate())
                .append("（共").append(days).append("天）\n");
        sb.append("人数：").append(trip.getPeopleCount()).append("人\n");
        if (trip.getBudget() != null) {
            sb.append("总预算：").append(trip.getBudget()).append("元\n");
        }
        if (StringUtils.hasText(trip.getPreferences())) {
            sb.append("偏好：").append(trip.getPreferences()).append("\n");
        }
        sb.append("请输出 JSON，结构为：\n");
        sb.append("{\"summary\":\"一句话总览\",\"estimatedCost\":数字,\"advice\":\"总览建议\",");
        sb.append("\"days\":[{\"day\":1,\"date\":\"yyyy-MM-dd\",\"theme\":\"主题\",");
        sb.append("\"items\":[{\"slot\":\"morning|noon|afternoon|evening|night\",\"type\":\"景点|餐饮|住宿|交通\",");
        sb.append("\"name\":\"名称\",\"description\":\"说明\",\"lng\":经度,\"lat\":纬度,\"cost\":花费}]}]}\n");
        sb.append("lng/lat 尽量填目的地真实坐标；cost 为单人参考花费（元）。");
        return sb.toString();
    }

    /** 无外部 API 时的确定性兜底方案 */
    public PlanResult localPlan(Trip trip) {
        long dayCount = java.time.temporal.ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        if (dayCount < 1) {
            dayCount = 1;
        }
        if (dayCount > 15) {
            dayCount = 15;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<PlanResult.DayPlan> days = new ArrayList<>();
        double total = 0;
        String dest = trip.getDestination();
        String[] themes = {"初见" + dest + " · 城市地标", dest + " · 人文历史", dest + " · 自然风光",
                dest + " · 街巷美食", dest + " · 休闲购物", dest + " · 周边精选", dest + " · 慢生活"};

        // 尝试从高德获取 POI 坐标
        Map<String, double[]> poi = amapService.searchPoi(dest);

        for (int d = 1; d <= dayCount; d++) {
            PlanResult.DayPlan day = new PlanResult.DayPlan();
            day.setDay(d);
            day.setDate(trip.getStartDate().plusDays(d - 1).format(fmt));
            day.setTheme(themes[(d - 1) % themes.length]);
            List<PlanResult.ScheduleItem> items = new ArrayList<>();

            PlanResult.ScheduleItem morning = item("morning", "景点", dest + "标志性景点", "上午游览" + dest + "代表性景点，建议 2-3 小时深度参观。", 0.0);
            PlanResult.ScheduleItem noon = item("noon", "餐饮", dest + "特色午餐", "品尝当地口碑菜系，人均约 50-80 元。", 50.0);
            PlanResult.ScheduleItem afternoon = item("afternoon", "景点", dest + "文化场馆/街区", "下午漫步历史街区或博物馆，感受城市文脉。", 0.0);
            PlanResult.ScheduleItem evening = item("evening", "餐饮", dest + "夜市/人气餐厅", "晚间打卡人气美食街，体验夜生活氛围。", 60.0);
            PlanResult.ScheduleItem night = item("night", "住宿", dest + "舒适型酒店", "入住交通便利的舒适型酒店，好好休息。", 180.0);

            if (d == 1) {
                morning.setName("抵达" + dest + " · 入住酒店");
                morning.setType("交通");
                morning.setDescription("抵达目的地，酒店办理入住，稍作休整后附近漫步。");
                night.setName(dest + "市区酒店（首晚）");
            }
            if (d == dayCount) {
                afternoon.setName("返程准备 · 伴手礼");
                evening.setName("告别" + dest + " · 返程");
                evening.setType("交通");
                evening.setDescription("收拾行李，购买伴手礼，前往车站/机场返程。");
                night.setName("返程途中");
                night.setCost(BigDecimal.ZERO);
            }

            if (poi != null) {
                double[] c = poi.get("lnglat");
                if (c != null) {
                    morning.setLng(c[0]);
                    morning.setLat(c[1]);
                    afternoon.setLng(c[0]);
                    afternoon.setLat(c[1]);
                }
                String img = poi.get("image") != null ? null : null;
            }

            items.add(morning);
            items.add(noon);
            items.add(afternoon);
            items.add(evening);
            items.add(night);
            day.setItems(items);
            days.add(day);
            for (PlanResult.ScheduleItem si : items) {
                if (si.getCost() != null) {
                    total += si.getCost().doubleValue();
                }
            }
        }

        PlanResult result = new PlanResult();
        result.setSummary(String.format("%s %d日行程：覆盖核心景点、特色餐饮与舒适住宿，节奏适中、适合自由行。", dest, dayCount));
        BigDecimal estimated = BigDecimal.valueOf(Math.round(total * trip.getPeopleCount()));
        result.setEstimatedCost(estimated);
        result.setAdvice("建议提前查看天气并预约热门景点；出行前确认交通班次，预留弹性时间。预算仅供参考，可按实际消费调整。");
        result.setDays(days);
        return result;
    }

    private PlanResult.ScheduleItem item(String slot, String type, String name, String desc, double cost) {
        PlanResult.ScheduleItem si = new PlanResult.ScheduleItem();
        si.setSlot(slot);
        si.setType(type);
        si.setName(name);
        si.setDescription(desc);
        si.setCost(BigDecimal.valueOf(cost));
        return si;
    }
}
