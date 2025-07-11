package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.Login;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "查找乘客端当前订单")
    @Login
    @GetMapping("/searchCustomerCurrentOrder")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(){
        CurrentOrderInfoVo currentOrderInfoVo = orderService.searchCustomerCurrentOrder(AuthContextHolder.getUserId());
        return Result.ok(currentOrderInfoVo);
    }


    @Operation(summary = "估计订单数据")
    @Login
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }


    @Operation(summary = "乘客下单")
    @Login
    @PostMapping("/submitOrder")
    public Result<Long> saveOrderInfo(@RequestBody SubmitOrderForm submitOrderForm){
        submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }


    @Operation(summary = "查询订单状态")
    @Login
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "获取订单信息")
    @Login
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderInfo(AuthContextHolder.getUserId(),orderId));
    }


    @Operation(summary = "获取司机基本信息")
    @Login
    @GetMapping("/getDriverInfo/{orderId}")
    public Result<DriverInfoVo> getDriverInfoOrder(@PathVariable Long orderId){
        return Result.ok(orderService.getDriverInfo(orderId,AuthContextHolder.getUserId()));
    }

    @Operation(summary = "司机赶往代驾起始点，获取订单经纬度位置")
    @Login
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getOrderLocation(@PathVariable Long orderId){
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }

    @Operation(summary = "计算最佳驾驶路线")
    @Login
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm){
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "代驾服务：获取订单最后一个位置信息")
    @Login
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }

    @Operation(summary = "获取乘客订单分页列表")
    @Login
    @GetMapping("/findCustomerOrderPage/{page}/{limit}")
    public Result<PageVo> findCustomerOrderPage(
            @Parameter(name = "page",description = "当前页码",required = true)
            @PathVariable Long page,
            @Parameter(name = "limit",description = "每页记录数",required = true)
            @PathVariable Long limit
    ){
       PageVo pageVo = orderService.findCustomerOrderPage(AuthContextHolder.getUserId(),page,limit);
       return Result.ok(pageVo);
    }

    @Operation(summary = "创建微信支付")
    @PostMapping("/createWxPayment")
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm){
        createWxPaymentForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }

    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    @Login
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo){
        return Result.ok(orderService.queryPayStatus(orderNo));
    }

}

