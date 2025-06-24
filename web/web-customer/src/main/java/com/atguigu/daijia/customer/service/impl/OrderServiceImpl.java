package com.atguigu.daijia.customer.service.impl;

import java.math.BigDecimal;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.map.client.WxPayFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.NewOrderFeignClient;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    private final MapFeignClient mapFeignClient;

    private final FeeRuleFeignClient feeRuleFeignClient;

    private final OrderInfoFeignClient orderInfoFeignClient;

    private final NewOrderFeignClient newOrderFeignClient;

    private final DriverInfoFeignClient driverInfoFeignClient;

    private final LocationFeignClient locationFeignClient;

    private final CustomerInfoFeignClient customerInfoFeignClient;

    private final WxPayFeignClient wxPayFeignClient;

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        // 获取驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtil.copyProperties(expectOrderForm, calculateDrivingLineForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        // 获取订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo ruleResponseVo = feeRuleResponseVoResult.getData();

        // 封装ExpectOrderVo
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(drivingLineVo);
        expectOrderVo.setFeeRuleResponseVo(ruleResponseVo);
        return expectOrderVo;
    }

    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {
        // 重新计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm = BeanUtil.copyProperties(submitOrderForm, CalculateDrivingLineForm.class);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        // 重新获取订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo ruleResponseVo = feeRuleResponseVoResult.getData();

        // 封装数据
        OrderInfoForm orderInfoForm = BeanUtil.copyProperties(submitOrderForm, OrderInfoForm.class);
        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
        orderInfoForm.setExpectAmount(ruleResponseVo.getTotalAmount());

        Long orderId = orderInfoFeignClient.saveOrderInfo(orderInfoForm).getData();

        // 查询附近可以接单的司机
        NewOrderTaskVo newOrderTaskVo = new NewOrderTaskVo();
        newOrderTaskVo.setOrderId(orderId);
        newOrderTaskVo.setStartLocation(orderInfoForm.getStartLocation());
        newOrderTaskVo.setStartPointLongitude(orderInfoForm.getStartPointLongitude());
        newOrderTaskVo.setStartPointLatitude(orderInfoForm.getStartPointLatitude());
        newOrderTaskVo.setEndLocation(orderInfoForm.getEndLocation());
        newOrderTaskVo.setEndPointLongitude(orderInfoForm.getEndPointLongitude());
        newOrderTaskVo.setEndPointLatitude(orderInfoForm.getEndPointLatitude());
        newOrderTaskVo.setExpectAmount(orderInfoForm.getExpectDistance());
        newOrderTaskVo.setExpectDistance(orderInfoForm.getExpectDistance());
        newOrderTaskVo.setExpectTime(drivingLineVo.getDuration());
        newOrderTaskVo.setFavourFee(orderInfoForm.getFavourFee());
        newOrderTaskVo.setCreateTime(new Date());

        // 返回jobId
        Long jobId = newOrderFeignClient.addAndStartTask(newOrderTaskVo).getData();

        // 返回订单id
        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {

        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        return orderStatus.getData();
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long userId) {
        return orderInfoFeignClient.searchCustomerCurrentOrder(userId).getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long userId, Long orderId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();

        if (!Objects.equals(orderInfo.getCustomerId(), userId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 获取司机信息
        DriverInfoVo driverInfo = null;
        Long driverId = orderInfo.getDriverId();
        if (driverId != null){
            driverInfo = driverInfoFeignClient.getDriverInfoOrder(driverId).getData();
        }

        // 获取账单信息
        OrderBillVo orderBillVo = null;
        if (orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()){
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        }

        OrderInfoVo orderInfoVo = BeanUtil.copyProperties(orderInfo, OrderInfoVo.class);
        orderInfoVo.setOrderId(orderId);
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setDriverInfoVo(driverInfo);

        return orderInfoVo;
    }

    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long userId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if (orderInfo.getCustomerId() != userId) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverInfoFeignClient.getDriverInfoOrder(orderInfo.getDriverId()).getData();
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return locationFeignClient.getCacheOrderLocation(orderId).getData();

    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
    }

    @Override
    public PageVo findCustomerOrderPage(Long userId, Long page, Long limit) {
        return orderInfoFeignClient.findCustomerOrderPage(userId,page,limit).getData();
    }

    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        // 获取订单支付信息
        OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(), createWxPaymentForm.getCustomerId()).getData();
        // 判断
        if (orderPayVo.getStatus() != OrderStatus.UNPAID.getStatus()){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 获取乘客司机openId
        String customerOpenId = customerInfoFeignClient.getCustomerOpenId(createWxPaymentForm.getCustomerId()).getData();
        String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

        // 封装需要数据到实体类，远程调用发起微信支付
        PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
        paymentInfoForm.setCustomerOpenId(customerOpenId);
        paymentInfoForm.setDriverOpenId(driverOpenId);
        paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
        paymentInfoForm.setAmount(orderPayVo.getPayAmount());
        paymentInfoForm.setContent(orderPayVo.getContent());
        paymentInfoForm.setPayWay(1);

        return wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        return wxPayFeignClient.queryPayStatus(orderNo).getData();
    }
}
