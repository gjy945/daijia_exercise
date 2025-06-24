package com.atguigu.daijia.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jodd.time.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Var;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private final OrderInfoMapper orderInfoMapper;

    private final OrderStatusLogMapper orderStatusLogMapper;

    private final RedisTemplate redisTemplate;

    private final RedissonClient redissonClient;

    private final OrderMonitorService orderMonitorService;

    private final OrderBillMapper orderBillMapper;

    private final OrderProfitsharingMapper orderProfitsharingMapper;

    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoForm, OrderInfo.class);
        // 设置订单号
        orderInfo.setOrderNo(IdUtil.simpleUUID());
        // 设置订单状态
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());

        orderInfoMapper.insert(orderInfo);

        // 生成订单之后，发送延迟消息
        this.sendDelayMessage(orderInfo.getId());

        // 记录日志
        this.log(orderInfo.getId(), orderInfo.getStatus());

        // 向redis中放一个标识
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId().toString(), "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);

        return orderInfo.getId();
    }

    private void sendDelayMessage(Long id) {
        try {
            // 1 创建一个队列
            RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");

            // 2 把创建的队列放到延迟队列中
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

            // 3 发送消息到延迟队列中
            delayedQueue.offer(id.toString(),15, TimeUnit.MINUTES);
            // 设置过期时间
        }catch (Exception e){
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getId, orderId)
                .select(OrderInfo::getStatus);
        return Optional.ofNullable(orderInfoMapper.selectOne(queryWrapper))
                .map(OrderInfo::getStatus)
                .orElse(OrderStatus.NULL_ORDER.getStatus());
    }

    //司机端查找当前订单
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId, driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus, statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        // 判断订单是否存在，通过redis，减少数据库压力
        Boolean hasKey = redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK + orderId.toString());
        if (!hasKey) {
            // 抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            boolean b = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (b) {
                // 司机抢单
                // 修改order_info表订单状态值为2，已经接单 + 司机id + 司机接单时间
                // 修改条件： 根据订单id+司机id
                OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getId, orderId));
                // 设置
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                // 调用方法进行修改
                int i = orderInfoMapper.updateById(orderInfo);
                if (i != 1) {
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                // 删除抢单的标识
                return redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId.toString());
            }
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long driverId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getCustomerId, driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        queryWrapper.in(OrderInfo::getStatus, statusArray);
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last(" limit 1");

        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (!ObjectUtils.isEmpty(orderInfo)) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }

        return currentOrderInfoVo;
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 更新订单状态和到达时间

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, queryWrapper);

        if (rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        OrderInfo updateOrderInfo = BeanUtil.copyProperties(updateOrderCartForm, OrderInfo.class);
        updateOrderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        // 只能更新自己的订单
        int rows = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if (rows == 1) {
            // 记录日志
            this.log(updateOrderCartForm.getOrderId(), OrderStatus.UPDATE_CART_INFO.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        // 根据订单id和司机id更新订单状态
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());
        orderInfoMapper.update(orderInfo, wrapper);

        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);

        return orderInfoMapper.update(orderInfo, wrapper) == 1;
    }

    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .ge(OrderInfo::getStartServiceTime, startTime)
                .lt(OrderInfo::getStartServiceTime, endTime);
        return orderInfoMapper.selectCount(wrapper);
    }

    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        // 更新订单信息
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, updateOrderBillForm.getOrderId())
                .eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        boolean update = this.update(orderInfo, wrapper);

        if (update) {
            // 添加账单信息
            OrderBill orderBill = BeanUtil.copyProperties(updateOrderBillForm, OrderBill.class);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            // 添加分帐信息
            OrderProfitsharing orderProfitsharing = BeanUtil.copyProperties(updateOrderBillForm, OrderProfitsharing.class);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);

        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        return true;
    }

    @Override
    public PageVo<OrderListVo> findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> orderInfoPage = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo<>(orderInfoPage.getRecords(), orderInfoPage.getPages(), orderInfoPage.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        return BeanUtil.copyProperties(orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderId)), OrderBillVo.class);
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        return BeanUtil.copyProperties(orderProfitsharingMapper.selectOne(new LambdaQueryWrapper<OrderProfitsharing>().eq(OrderProfitsharing::getOrderId, orderId)), OrderProfitsharingVo.class);
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        // 更新订单信息
        LambdaQueryWrapper<OrderInfo> wrapper = Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getDriverId, driverId);
        // 更新字段
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.UNPAID.getStatus());

        // 只能更新自己的订单
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            this.log(orderId, OrderStatus.UNPAID.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        return true;
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo,customerId);
        if (orderPayVo != null){
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (orderInfo == null || orderInfo.getStatus() == OrderStatus.PAID.getStatus()){
            return true;
        }

        // 更新状态
        orderInfo.setStatus(OrderStatus.PAID.getStatus());
        orderInfo.setPayTime(new Date());
        int rows = orderInfoMapper.updateById(orderInfo);
        if (rows == 1){
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        // 根据订单编号查询订单表
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo)
                .select(OrderInfo::getId, OrderInfo::getDriverId));

        // 根据订单编号查询系统奖励表
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                .eq(OrderBill::getOrderId, orderInfo.getId())
                .select(OrderBill::getRewardFee));

        // 封装到vo里面
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    @Override
    public void orderCancel(Long l) {
        // orderId查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(l);
        if (orderInfo.getStatus() == OrderStatus.WAITING_ACCEPT.getStatus()){
            // 取消订单
            orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
            int rows = orderInfoMapper.updateById(orderInfo);
            if (rows == 1){
                // 删除接单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + l.toString());
            }
        }
    }

    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
