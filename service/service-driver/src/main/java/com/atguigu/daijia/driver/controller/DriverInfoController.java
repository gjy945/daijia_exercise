package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {


    private final DriverInfoService driverInfoService;


    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable String code){
        return Result.ok(driverInfoService.login(code));
    }

    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo/{driverId}")
    public Result<DriverLoginVo> getDriverInfo(@PathVariable Long driverId){
        return Result.ok(driverInfoService.getDriverInfo(driverId));
    }

    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo/{driverId}")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId) throws Exception {
        return Result.ok(driverInfoService.getDriverAuthInfo(driverId));
    }

    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm){
        return Result.ok(driverInfoService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }

    @Operation(summary = "创建司机人脸模型")
    @PostMapping(value = "/creatDriverFaceModel")
    public Result<Boolean> createDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm){
        return Result.ok(driverInfoService.createDriverFaceModel(driverFaceModelForm));
    }

    @Operation(summary = "查询司机个性化设置信息")
    @GetMapping("/getDriverSet/{driverId}")
    public Result<List<DriverSet>> getDriverSet(@PathVariable String driverId){
        List<Long> idList = Arrays.stream(driverId.split(","))
                .map(Long::valueOf)
                .toList();
        return Result.ok(driverInfoService.getDriverSet(idList));
    }

    @Operation(summary = "判断司机当日是否进行了人脸识别")
    @GetMapping("/isFaceRecognition/{driverId}")
    public Result<Boolean> isFaceRecognition(@PathVariable("driverId") Long driverId){
        return Result.ok(driverInfoService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm){
        return Result.ok(driverInfoService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "更新接单状态")
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    public Result<Boolean> updateServiceStatus(@PathVariable Long driverId,@PathVariable Integer status){
        return Result.ok(driverInfoService.updateServiceStatus(driverId,status));
    }

    @Operation(summary = "获取司机基本信息")
    @GetMapping("/getDriverInfo/{driverId}")
    public Result<DriverInfoVo> getDriverInfoOrder(@PathVariable Long driverId){
        return Result.ok(driverInfoService.getDriverInfoOrder(driverId));
    }

    @Operation(summary = "获取司机openId")
    @GetMapping("/getDriverOpenId/{driverId}")
    public Result<String> getDriverOpenId(@PathVariable Long driverId){
        return Result.ok(driverInfoService.getDriverOpenId(driverId));
    }

}

