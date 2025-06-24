package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;

import java.util.List;

public interface LocationService {


    /**
     * 司机开启接单服务，更新司机位置信息
     * @param updateDriverLocationForm
     * @return
     */
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    /**
     * 司机关闭接单服务，删除司机经纬度位置
     * @param userId
     * @return
     */
    Boolean removeDriverLocation(Long userId);

    /**
     * 司机赶往代驾起始点，更新订单地址到缓存
     * @param updateOrderLocationForm
     * @return
     */
    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

    /**
     * 开始代驾：保存代驾服务订单位置信息
     * @param orderServiceLocationForms
     * @return
     */
    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms);
}
