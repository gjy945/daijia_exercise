package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {


    /**
     * 微信小程序授权登录
     * @param code 授权码
     * @return token
     */
    String login(String code);


    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return 登录信息
     */
    DriverLoginVo getDriverLoginInfo(Long driverId);


    /**
     * 获取司机登录认证信息
     * @param userId
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long userId);


    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm
     * @return
     */
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);


    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm
     * @return
     */
    Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    /**
     * 判断司机当日是否进行了人脸识别
     * @param userId
     * @return
     */
    Boolean isFaceRecognition(Long userId);

    /**
     * 验证司机人脸
     * @param driverFaceModelForm
     * @return
     */
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    /**
     * 开始接单服务
     * @param userId
     * @return
     */
    Boolean startService(Long userId);


    /**
     * 停止接单服务
     * @param userId
     * @return
     */
    Boolean stopService(Long userId);

    /**
     * 获取司机openId
     * @param userId
     * @return
     */
    String getDriverOpenId(Long userId);
}
