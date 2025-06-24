package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpStatus;
import com.atguigu.daijia.common.config.redis.RedisConfig;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.order.client.NewOrderFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    private final DriverInfoFeignClient driverInfoFeignClient;

    private final RedisTemplate redisTemplate;

    private final LocationFeignClient locationFeignClient;

    private final NewOrderFeignClient newOrderFeignClient;

    @Override
    public String login(String code) {

        Result<Long> login = driverInfoFeignClient.login(code);
        if (login.getCode() != HttpStatus.HTTP_OK) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 防止数据为空
        Long driverId = Optional.ofNullable(login.getData()).orElseThrow(
                () -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        // 生成token，存到redis中
        String token = IdUtil.simpleUUID();
        redisTemplate.opsForValue().set(
                RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.MINUTES);

        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {

        Result<DriverLoginVo> driverInfo = driverInfoFeignClient.getDriverInfo(driverId);
        if (driverInfo.getCode() != HttpStatus.HTTP_OK) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        DriverLoginVo driverLoginVo = Optional.ofNullable(driverInfo.getData()).orElseThrow(
                () -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long userId) {
        Result<DriverAuthInfoVo> driverAuthInfo = driverInfoFeignClient.getDriverAuthInfo(userId);
        if (driverAuthInfo.getCode() != HttpStatus.HTTP_OK){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverAuthInfo.getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.updateDriverAuthInfo(updateDriverAuthInfoForm);
        if (booleanResult.getCode() != HttpStatus.HTTP_OK){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return booleanResult.getData();
    }

    @Override
    public Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {

        Result<Boolean> driverFaceModel = driverInfoFeignClient.createDriverFaceModel(driverFaceModelForm);
        if (driverFaceModel.getCode() != HttpStatus.HTTP_OK){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverFaceModel.getData();
    }

    @Override
    public Boolean isFaceRecognition(Long userId) {
        return driverInfoFeignClient.isFaceRecognition(userId).getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    @Override
    public Boolean startService(Long userId) {
        // 判断完成认证
        DriverLoginVo driverLoginInfo = this.getDriverLoginInfo(userId);
        if (driverLoginInfo.getAuthStatus() != 2){
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        // 判断当日是否人脸识别
        Boolean isFace = driverInfoFeignClient.isFaceRecognition(userId).getData();
        if (!isFace){
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        // 更新订单状态 1 开始接单
        driverInfoFeignClient.updateServiceStatus(userId,1);

        // 删除redis司机位置信息
        locationFeignClient.removeDriverLocation(userId);

        // 清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(userId);
        return true;
    }

    @Override
    public Boolean stopService(Long userId) {
        // 更新订单状态 0 停止接单
        driverInfoFeignClient.updateServiceStatus(userId,0);

        // 删除redis司机位置信息
        locationFeignClient.removeDriverLocation(userId);

        // 清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(userId);

        return true;
    }

    @Override
    public String getDriverOpenId(Long userId) {
        return driverInfoFeignClient.getDriverOpenId(userId).getData();
    }
}
