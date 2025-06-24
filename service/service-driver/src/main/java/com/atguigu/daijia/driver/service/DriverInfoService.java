package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

public interface DriverInfoService extends IService<DriverInfo> {

    /**
     * 小程序授权登录
     * @param code 授权码
     * @return 用户id
     */
    Long login(String code);


    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return 登录信息
     */
    DriverLoginVo getDriverInfo(Long driverId);


    /**
     * 获取司机认证信息
     * @param driverId
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId) throws Exception;


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
     * 查询司机个性化设置信息
     * @param driverId
     * @return
     */
    List<DriverSet> getDriverSet(Collection<Long> driverId);

    /**
     * 判断司机当日是否进行了人脸识别
     * @param driverId
     * @return
     */
    Boolean isFaceRecognition(Long driverId);

    /**
     * 验证司机人脸
     * @param driverFaceModelForm
     * @return
     */
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    /**
     * 更新接单状态
     * @param driverId
     * @param status
     * @return
     */
    Boolean updateServiceStatus(Long driverId, Integer status);

    /**
     * 获取司机基本信息
     * @param driverId
     * @return
     */
    DriverInfoVo getDriverInfoOrder(Long driverId);

    /**
     * 获取司机openId
     * @param driverId
     * @return
     */
    String getDriverOpenId(Long driverId);
}
