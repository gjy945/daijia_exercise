package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderService {


    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    /**
     * 查询司机新订单数据
     * @param userId
     * @return
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long userId);

    /**
     * 司机端查找当前订单
     * @param driverId
     * @return
     */
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    /**
     * 司机抢单
     * @param userId
     * @param orderId
     * @return
     */
    Boolean robNewOrder(Long userId, Long orderId);

    /**
     * 获取订单信息
     * @param userId
     * @param orderId
     * @return
     */
    OrderInfoVo getOrderInfo(Long userId, Long orderId);

    /**
     * 计算最佳驾驶路线
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calcuateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    /**
     * 司机到达起始点
     * @param orderId
     * @param userId
     * @return
     */
    Boolean driverArriveStartLocation(Long orderId, Long userId);

    /**
     * 更新代驾车辆信息
     * @param updateOrderCartForm
     * @return
     */
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    /**
     * 开始代驾服务
     * @param startDriveForm
     * @return
     */
    Boolean startDrive(StartDriveForm startDriveForm);

    /**
     * 结束代驾服务更新订单账单
     * @param orderFeeForm
     * @return
     */
    Boolean endDrive(OrderFeeForm orderFeeForm);

    /**
     * 结束代驾服务更新订单账单（多线程优化）
     * @param orderFeeForm
     * @return
     */
    Boolean endDriveThread(OrderFeeForm orderFeeForm);

    /**
     * 获取司机订单分页列表
     * @param userId
     * @param page
     * @param limit
     * @return
     */
    PageVo findDriverOrderPage(Long userId, Long page, Long limit);

    /**
     * 发送账单信息
     * @param orderId
     * @param userId
     * @return
     */
    Boolean sendOrderBillInfo(Long orderId, Long userId);
}
