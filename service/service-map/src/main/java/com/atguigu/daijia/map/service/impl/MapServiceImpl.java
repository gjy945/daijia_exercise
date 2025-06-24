package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.JsonObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    private final RestTemplate restTemplate;

    @Value("${tencent.map.key}")
    private String key;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        // 请求腾旭提供的接口，按要求传递相关参数，返回需要的结果
        // 使用RestTemplate
        // 构造请求URL
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        // 封装传递参数
        Map<String, String> map = new HashMap<>();
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        map.put("key", key);

        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        // 处理返回结果
        int status = result.getIntValue("status");
        if (status != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }

        // 获取返回路线信息
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);

        // 创建vo对象
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        // 预估时间
        drivingLineVo.setDistance(route.getBigDecimal("duration"));
        // 距离
        drivingLineVo.setDistance(
                route.getBigDecimal("distance")
                        .divide(new BigDecimal(1000))
                        .setScale(2, RoundingMode.HALF_UP));
        // 路线
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));

        return drivingLineVo;
    }
}
