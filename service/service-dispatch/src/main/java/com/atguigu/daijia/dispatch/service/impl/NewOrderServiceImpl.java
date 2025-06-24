package com.atguigu.daijia.dispatch.service.impl;

import java.math.BigDecimal;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

    private final XxlJobClient xxlJobClient;

    private final OrderJobMapper orderJobMapper;

    private final LocationFeignClient locationFeignClient;

    private final OrderInfoFeignClient orderInfoFeignClient;

    private final RedisTemplate redisTemplate;

    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        // 判断当前订单是否启动任务调度
        LambdaQueryWrapper<OrderJob> queryWrapper = Wrappers.<OrderJob>lambdaQuery()
                .eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId());
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);

        // 没有启动，进行操作
        if (ObjectUtils.isEmpty(orderJob)) {
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler",
                    "",
                    "0 0/1 * * * ?",
                    "新创建订单任务调度：" + newOrderTaskVo.getOrderId());

            // 记录任务调度信息
            orderJob = new OrderJob();
            orderJob.setJobId(jobId);
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }

        return orderJob.getJobId();
    }

    @Override
    public void executeTask(long jobId) {
        // 1. 根据jobId查询数据库表，当前任务是否已经创建
        // 如果没有创建，不往下执行
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getJobId, jobId));
        if (orderJob == null) {
            return;
        }

        // 2. 查询当前订单状态，如果当前订单为接单状态，继续执行，如果当前订单不是接单状态，停止任务调度
        // 获取orderJob里面的对象
        String parameter = orderJob.getParameter();
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(parameter, NewOrderTaskVo.class);
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(newOrderTaskVo.getOrderId());
        Integer status = orderStatus.getData();
        if (status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            // 停止任务调度
            xxlJobClient.stopJob(jobId);
            return;
        }

        // 3. 远程调用，搜索附近满足条件可以接单的司机
        // 4. 远程调用之后，获取满足可以接单的司机集合
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());

        Collection<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();

        if (nearByDriverVoList == null || nearByDriverVoList.isEmpty()){
            return;
        }

        // 5. 遍历司机集合，得到每个司机，为每个司机创建临时队列，存储新订单信息
        nearByDriverVoList.forEach(nearByDriverVo -> {
            // 使用redis的set类型
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId();

            // 记录司机id，防止重复推送
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, nearByDriverVo.getDriverId());
            if (!isMember) {
                // 把订单信息推送给满足条件的多个司机
                redisTemplate.opsForSet().add(repeatKey, nearByDriverVo.getDriverId());
                // 过期时间： 15分钟，超过15分钟没有接单自动取消
                redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setDistance(nearByDriverVo.getDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());
                // 新订单保存司机的临时队列，redis里面的list集合
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + nearByDriverVo.getDriverId();
                redisTemplate.opsForList().leftPush(key, JSONObject.toJSONString(newOrderDataVo));
                // 过期时间： 1分钟
                redisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

            }


        });

    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        Long size = redisTemplate.opsForList().size(key);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                String content = (String) redisTemplate.opsForList().leftPop(key);
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);

        return true;
    }
}
