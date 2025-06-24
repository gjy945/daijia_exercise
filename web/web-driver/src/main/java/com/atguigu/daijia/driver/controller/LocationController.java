package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.Login;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping(value="/location")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

    private final LocationService locationService;


    @Operation(summary = "司机开启接单服务，更新司机位置信息")
    @Login
    @PostMapping("/updateDriverLocation")
    public Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm){
        updateDriverLocationForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(locationService.updateDriverLocation(updateDriverLocationForm));
    }


    @Operation(summary = "司机关闭接单服务，删除司机经纬度位置")
    @Login
    @DeleteMapping("/removeDriverLocation")
    public Result<Boolean> removeDriverLocation(){
        return Result.ok(locationService.removeDriverLocation(AuthContextHolder.getUserId()));
    }


    @Operation(summary = "司机赶往代驾起始点，更新订单地址到缓存")
    @Login
    @PostMapping("/updateOrderLocationToCache")
    public Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm){
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }

    @Operation(summary = "开始代驾：保存代驾服务订单位置信息")
    @Login
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderServiceLocationForms){
        return Result.ok(locationService.saveOrderServiceLocation(orderServiceLocationForms));
    }

}

