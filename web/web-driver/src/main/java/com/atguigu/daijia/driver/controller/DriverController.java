package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.Login;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequiredArgsConstructor
@RequestMapping(value="/driver")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverController {

    private final DriverService driverService;


    @Operation(summary = "微信小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code){
        return Result.ok(driverService.login(code));
    }

    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    @Login
    public Result<DriverLoginVo> getDriverLoginInfo(){
        // 获取到当前登录用户id
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverLoginInfo(driverId));
    }

    @Operation(summary = "获取司机认证信息")
    @Login
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(){
        return Result.ok(driverService.getDriverAuthInfo(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "更新司机认证信息")
    @Login
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm){
        updateDriverAuthInfoForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }


    @Operation(summary = "创建司机人脸模型")
    @Login
    @PostMapping(value = "/creatDriverFaceModel")
    public Result<Boolean> createDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm){
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.createDriverFaceModel(driverFaceModelForm));
    }

    @Operation(summary = "判断司机当日是否进行了人脸识别")
    @Login
    @GetMapping("/isFaceRecognition")
    public Result<Boolean> isFaceRecognition(){
        return Result.ok(driverService.isFaceRecognition(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "验证司机人脸")
    @Login
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm){
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "开始接单服务")
    @Login
    @GetMapping("/startService")
    public Result<Boolean> startService(){
        return Result.ok(driverService.startService(AuthContextHolder.getUserId()));
    }


    @Operation(summary = "停止接单服务")
    @Login
    @GetMapping("/stopService")
    public Result<Boolean> stopService(){
        return Result.ok(driverService.stopService(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "获取司机openId")
    @GetMapping("/getDriverOpenId")
    public Result<String> getDriverOpenId(){
        return Result.ok(driverService.getDriverOpenId(AuthContextHolder.getUserId()));
    }
}

