package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.NewOrderFeignClient;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {


    private final OrderInfoFeignClient orderInfoFeignClient;

    private final NewOrderFeignClient newOrderFeignClient;

    private final MapFeignClient mapFeignClient;

    private final LocationFeignClient locationFeignClient;

    private final FeeRuleFeignClient feeRuleFeignClient;

    private final RewardRuleFeignClient rewardRuleFeignClient;

    private final ProfitsharingRuleFeignClient profitsharingRuleFeignClient;

    private final ThreadPoolExecutor threadPoolExecutor;

    @Override
    public Integer getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long userId) {
        Result<List<NewOrderDataVo>> newOrderQueueData = newOrderFeignClient.findNewOrderQueueData(userId);
        return newOrderQueueData.getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
    }

    @Override
    public Boolean robNewOrder(Long userId, Long orderId) {
        return orderInfoFeignClient.robNewOrder(userId, orderId).getData();
    }

    @Override
    @SneakyThrows
    public OrderInfoVo getOrderInfo(Long userId, Long orderId) {
        CompletableFuture<OrderInfo> orderInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();

            if (!Objects.equals(orderInfo.getDriverId(), userId)) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            return orderInfo;
        });

        // 获取账单和分账信息，封装到vo中
        CompletableFuture<OrderBillVo> orderBillVoCompletableFuture = CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(orderInfoFeignClient.getOrderBillInfo(orderId).getData()).orElse(new OrderBillVo()));
        CompletableFuture<OrderProfitsharingVo> orderProfitsharingVoCompletableFuture = CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(orderInfoFeignClient.getOrderProfitsharing(orderId).getData()).orElse(new OrderProfitsharingVo()));

        // 合并CompletableFuture
        CompletableFuture.allOf(orderInfoCompletableFuture,orderBillVoCompletableFuture,orderProfitsharingVoCompletableFuture);

        OrderInfoVo orderInfoVo = BeanUtil.copyProperties(orderInfoCompletableFuture.get(), OrderInfoVo.class);
        orderInfoVo.setOrderId(orderId);
        orderInfoVo.setOrderBillVo(orderBillVoCompletableFuture.get());
        orderInfoVo.setOrderProfitsharingVo(orderProfitsharingVoCompletableFuture.get());

        return orderInfoVo;
    }

    @Override
    public DrivingLineVo calcuateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long userId) {
        // 判断司机是否刷单
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();

        // 司机当前位置
        OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();

        // 司机当前位置和代驾开始位置距离
        double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(),
                orderInfo.getStartPointLongitude().doubleValue(),
                orderLocationVo.getLatitude().doubleValue(),
                orderLocationVo.getLongitude().doubleValue());
        if (distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
        }

        return orderInfoFeignClient.driverArriveStartLocation(orderId, userId).getData();
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        return orderInfoFeignClient.updateOrderCart(updateOrderCartForm).getData();
    }

    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        return orderInfoFeignClient.startDrive(startDriveForm).getData();
    }

    @Override
    public PageVo findDriverOrderPage(Long userId, Long page, Long limit) {
        return orderInfoFeignClient.findDriverOrderPage(userId, page, limit).getData();
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long userId) {
        return orderInfoFeignClient.sendOrderBillInfo(orderId,userId).getData();
    }


    @Override
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        // 1. 根据orderId获取订单信息，判断当前订单是否为自己接的单
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
        if (orderInfo.getDriverId() != orderFeeForm.getDriverId()) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        // 防止刷单
        OrderServiceLastLocationVo orderServiceLastLocationVo = locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();

        // 计算司机当前位置距离结束代驾位置
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                orderInfo.getEndPointLongitude().doubleValue(),
                orderServiceLastLocationVo.getLatitude().doubleValue(),
                orderServiceLastLocationVo.getLongitude().doubleValue());

        if (distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        // 2. 订单实际的里程
        BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();

        // 3. 计算代驾实际的费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(realDistance);
        feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        feeRuleRequestForm.setWaitMinute(Math.abs((int) (orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / 1000 * 60));

        FeeRuleResponseVo ruleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
        // 实际费用 = 代驾费用 + 其他费用（过路费，停车费）
        BigDecimal totalAmount = ruleResponseVo.getTotalAmount()
                .add(orderFeeForm.getTollFee())
                .add(orderFeeForm.getParkingFee())
                .add(orderFeeForm.getOtherFee())
                .add(orderInfo.getFavourFee());

        ruleResponseVo.setTotalAmount(totalAmount);

        // 4. 计算系统奖励
        String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
        String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
        Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
        // 封装参数
        RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
        rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        rewardRuleRequestForm.setOrderNum(orderNum);

        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();

        // 5. 计算分帐信息
        ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
        profitsharingRuleRequestForm.setOrderAmount(ruleResponseVo.getTotalAmount());
        profitsharingRuleRequestForm.setOrderNum(orderNum);
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();

        // 6. 封装实体类，调用接口，结束代驾，添加账单和分帐信息
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        updateOrderBillForm.setRealDistance(realDistance);
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

        // 订单奖励信息
        BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);

        // 代驾费用信息
        BeanUtils.copyProperties(ruleResponseVo, updateOrderBillForm);

        //分账相关信息
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
        return orderInfoFeignClient.endDrive(updateOrderBillForm).getData();
    }


    // 使用多线程优化
    @SneakyThrows
    public Boolean endDriveThread(OrderFeeForm orderFeeForm) {
        // 1. 根据orderId获取订单信息，判断当前订单是否为自己接的单
        CompletableFuture<OrderInfo> orderInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
            if (orderInfo.getDriverId() != orderFeeForm.getDriverId()) {
                throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
            }
            return orderInfo;
        },threadPoolExecutor);


        // 防止刷单
        CompletableFuture<OrderServiceLastLocationVo> orderServiceLastLocationVoCompletableFuture = CompletableFuture.supplyAsync(
                () -> locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData(),threadPoolExecutor);

        // 上面两个合并
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(orderInfoCompletableFuture, orderServiceLastLocationVoCompletableFuture);
        voidCompletableFuture.join();

        // 获取线程执行之后的结果
        OrderInfo orderInfo = orderInfoCompletableFuture.get();
        OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationVoCompletableFuture.get();

        // 计算司机当前位置距离结束代驾位置
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                orderInfo.getEndPointLongitude().doubleValue(),
                orderServiceLastLocationVo.getLatitude().doubleValue(),
                orderServiceLastLocationVo.getLongitude().doubleValue());

        if (distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }



        // 2. 订单实际的里程
        CompletableFuture<BigDecimal> bigDecimalCompletableFuture = CompletableFuture.supplyAsync(
                () -> locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData(),threadPoolExecutor);
        // 3. 计算代驾实际的费用
        CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCompletableFuture = bigDecimalCompletableFuture.thenApplyAsync(realDistance -> {
            FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
            feeRuleRequestForm.setDistance(realDistance);
            feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            feeRuleRequestForm.setWaitMinute(Math.abs((int) (orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / 1000 * 60));

            FeeRuleResponseVo ruleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
            // 实际费用 = 代驾费用 + 其他费用（过路费，停车费）
            BigDecimal totalAmount = ruleResponseVo.getTotalAmount()
                    .add(orderFeeForm.getTollFee())
                    .add(orderFeeForm.getParkingFee())
                    .add(orderFeeForm.getOtherFee())
                    .add(orderInfo.getFavourFee());

            ruleResponseVo.setTotalAmount(totalAmount);
            return ruleResponseVo;
        },threadPoolExecutor);


        // 4. 计算系统奖励
        CompletableFuture<Long> orderNumCompletableFuture = CompletableFuture.supplyAsync(() -> {
            String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
            String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
            return orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
        },threadPoolExecutor);
        // 封装参数
        CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCompletableFuture = orderNumCompletableFuture.thenApplyAsync(orderNum -> {
            RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
            rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            rewardRuleRequestForm.setOrderNum(orderNum);

            return rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
        },threadPoolExecutor);


        // 5. 计算分帐信息
        CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCompletableFuture = feeRuleResponseVoCompletableFuture.thenCombineAsync(orderNumCompletableFuture, (ruleResponseVo, orderNum) -> {
            ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
            profitsharingRuleRequestForm.setOrderAmount(ruleResponseVo.getTotalAmount());
            profitsharingRuleRequestForm.setOrderNum(orderNum);
            return profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
        },threadPoolExecutor);


        // 合并
        CompletableFuture.allOf(
                orderInfoCompletableFuture,
                bigDecimalCompletableFuture,
                feeRuleResponseVoCompletableFuture,
                orderNumCompletableFuture,
                rewardRuleResponseVoCompletableFuture,
                profitsharingRuleResponseVoCompletableFuture
        ).join();


        // 6. 封装实体类，调用接口，结束代驾，添加账单和分帐信息
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        updateOrderBillForm.setRealDistance(bigDecimalCompletableFuture.get());
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

        // 订单奖励信息
        BeanUtils.copyProperties(rewardRuleResponseVoCompletableFuture.get(), updateOrderBillForm);

        // 代驾费用信息
        BeanUtils.copyProperties(feeRuleResponseVoCompletableFuture.get(), updateOrderBillForm);

        //分账相关信息
        BeanUtils.copyProperties(profitsharingRuleResponseVoCompletableFuture.get(), updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVoCompletableFuture.get().getProfitsharingRuleId());
        return orderInfoFeignClient.endDrive(updateOrderBillForm).getData();
    }
}
