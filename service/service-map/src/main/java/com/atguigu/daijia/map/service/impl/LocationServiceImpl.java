package com.atguigu.daijia.map.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.repository.OrderServiceLocationRepository;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    private final RedisTemplate redisTemplate;

    private final DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private OrderServiceLocationRepository orderServiceLocationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final OrderInfoFeignClient orderInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 把司机信息添加redis里面的geo
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        return Optional.ofNullable(
                redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString())
        ).orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR))
                > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        return Optional.ofNullable(
                redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString())
        ).orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR))
                > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        // 创建circle对象，point distance
        Circle circle = new Circle(
                new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue()),
                new Distance(5.0, RedisGeoCommands.DistanceUnit.KILOMETERS)
        );

        // 定义GEO参数，设置返回结果包含的内容
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance() // 包含距离
                .includeCoordinates() // 包含坐标
                .sortAscending(); // 升序排列

        GeoResults<RedisGeoCommands.GeoLocation<String>> radius = Optional.ofNullable(redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args))
                .orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = radius.getContent();

        // 获取司机id
        List<Long> list = content.stream().map(geoLocationGeoResult -> {
            // 司机id
            return Long.parseLong(geoLocationGeoResult.getContent().getName());
        }).toList();

        // 拼接成逗号分隔的字符串
        String driverIds = list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 远程调用，根据司机id个性化设置信息
        Result<List<DriverSet>> driverSet = driverInfoFeignClient.getDriverSet(driverIds);
        Collection<DriverSet> data = driverSet.getData();
        Map<Long, DriverSet> longDriverSetMap = data.stream().collect(Collectors.toMap(DriverSet::getDriverId, driverSet1 -> driverSet1));

        return content.stream().filter(geoLocationGeoResult -> {
            long driverId = Long.parseLong(geoLocationGeoResult.getContent().getName());
            // 拿到该司机的个性化设置信息
            DriverSet driverSet1 = longDriverSetMap.get(driverId);
            // 接单距离 - 当前距离 < 0 ，不符合条件
            if (driverSet1.getOrderDistance().doubleValue() != 0
                    && driverSet1.getOrderDistance().subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                return false;
            }

            // 接单里程设置
            BigDecimal acceptDistance = driverSet1.getAcceptDistance();
            double value = geoLocationGeoResult.getDistance().getValue();
            // 不符合条件
            if (acceptDistance.doubleValue() != 0 && acceptDistance.subtract(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)).doubleValue() < 0) {
                return false;
            }
            return true;
        }).map(geoLocationGeoResult -> {
            // 距离
            double value = geoLocationGeoResult.getDistance().getValue();
            NearByDriverVo nearByDriverVo = new NearByDriverVo();
            nearByDriverVo.setDriverId(Long.parseLong(geoLocationGeoResult.getContent().getName()));
            nearByDriverVo.setDistance(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP));
            return nearByDriverVo;
        }).toList();
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key, orderLocationVo);

        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return (OrderLocationVo) redisTemplate.opsForValue().get(RedisConstant.UPDATE_ORDER_LOCATION + orderId);
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms) {
        List<OrderServiceLocation> list = orderServiceLocationForms.stream().map(orderServiceLocationForm -> {
            OrderServiceLocation orderServiceLocation = BeanUtil.copyProperties(orderServiceLocationForm, OrderServiceLocation.class);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date());
            return orderServiceLocation;
        }).toList();

        orderServiceLocationRepository.saveAll(list);

        return true;
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {

        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("createTime")));
        query.limit(1);
        OrderServiceLocation serviceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);

        return BeanUtil.copyProperties(serviceLocation, OrderServiceLastLocationVo.class);
    }

    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        // 根据订单id获取代驾位置信息，根据创建时间排序（升序）
        // MongoRepository 方法 类似JPA？
        List<OrderServiceLocation> list = orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);

        double res = 0.0;

        // 遍历集合每个位置 ，计算两点距离 最后相加
        if (!CollectionUtils.isEmpty(list)){
            for (int i = 0,size = list.size() -1; i < size; i++) {
                OrderServiceLocation location1 = list.get(i);
                OrderServiceLocation location2 = list.get(i+1);

                // 计算距离
                double distance = LocationUtil.getDistance(location1.getLatitude().doubleValue(), location1.getLongitude().doubleValue(),
                        location2.getLatitude().doubleValue(), location2.getLatitude().doubleValue());
                res += distance;
            }
        }

        // TODO 测试 模拟数据
        if (res == 0){
            return orderInfoFeignClient.getOrderInfo(orderId).getData().getExpectDistance().add(new BigDecimal(5));
        }
        return new BigDecimal(res);
    }
}
