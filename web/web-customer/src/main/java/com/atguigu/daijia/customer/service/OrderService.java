package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;

public interface OrderService {

    /**
     * 估计订单数据
     * @param expectOrderForm
     * @return
     */
    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);


    /**
     * 乘客下单
     * @param submitOrderForm
     * @return
     */
    Long submitOrder(SubmitOrderForm submitOrderForm);

    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    /**
     * 查询乘客订单
     * @param userId
     * @return
     */
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long userId);

    /**
     * 获取订单信息
     * @param userId
     * @param orderId
     * @return
     */
    OrderInfoVo getOrderInfo(Long userId, Long orderId);

    /**
     * 获取司机基本信息
     * @param orderId
     * @param userId
     * @return
     */
    DriverInfoVo getDriverInfo(Long orderId, Long userId);

    /**
     * 司机赶往代驾起始点，获取订单经纬度位置
     * @param orderId
     * @return
     */
    OrderLocationVo getCacheOrderLocation(Long orderId);

    /**
     * 计算最佳驾驶路线
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    /**
     * 代驾服务：获取订单最后一个位置信息
     * @param orderId
     * @return
     */
    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    /**
     * 获取乘客订单分页列表
     * @param userId
     * @param page
     * @param limit
     * @return
     */
    PageVo findCustomerOrderPage(Long userId, Long page, Long limit);

    /**
     * 创建微信支付
     * @param createWxPaymentForm
     * @return
     */
    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

    /**
     * 支付状态查询
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);
}
