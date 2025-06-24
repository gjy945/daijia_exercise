package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    private final LocationFeignClient locationFeignClient;

    private final DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        List<Long> ids = Arrays.asList(updateDriverLocationForm.getDriverId());

        // 拼接成逗号分隔的字符串
        String driverIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 根基司机id虎丘司机个性化设置信息
        Result<List<DriverSet>> driverSet = driverInfoFeignClient.getDriverSet(driverIds);
        List<DriverSet> data = driverSet.getData();

        // 判断： 如果司机开始接单，更新位置信息
        if (data.get(0).getServiceStatus() == 1) {
            Result<Boolean> result = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
            return result.getData();
        } else {
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
    }

    @Override
    public Boolean removeDriverLocation(Long userId) {
        Result<Boolean> result = locationFeignClient.removeDriverLocation(userId);
        return result.getData();
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms) {
        return locationFeignClient.saveOrderServiceLocation(orderServiceLocationForms).getData();
    }
}
