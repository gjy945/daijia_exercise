package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.w3c.dom.CharacterData;

@FeignClient(value = "service-order")
public interface OrderInfoFeignClient {


    @PostMapping("/order/info/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    @GetMapping("/order/info/getOrderStatus/{orderId}")
    Result<Integer> getOrderStatus(@PathVariable Long orderId);

    @GetMapping("/order/info/robNewOrder/{driverId}/{orderId}")
    Result<Boolean> robNewOrder(@PathVariable Long driverId,@PathVariable Long orderId);

    @GetMapping("/order/info/searchDriverCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable("driverId") Long driverId);

    @GetMapping("/order/info/searchCustomerCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable Long driverId);

    @GetMapping("/order/info/getOrderInfo/{orderId}")
    Result<OrderInfo> getOrderInfo(@PathVariable Long orderId);

    @GetMapping("/order/info/driverArriveStartLocation/{orderId}/{driverId}")
    Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId,
                                                     @PathVariable Long driverId);

    @PostMapping("/order/info/updateOrderCart")
    Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm);

    @PostMapping("/order/info/startDrive")
    Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm);

    @GetMapping("/order/info/getOrderNumByTime/{startTime}/{endTime}")
    Result<Long> getOrderNumByTime(@PathVariable String startTime, @PathVariable String endTime);

    @PostMapping("/order/info/endDrive")
    Result<Boolean> endDrive(@RequestBody UpdateOrderBillForm updateOrderBillForm);

    @GetMapping("/order/info/findCustomerOrderPage/{customerId}/{page}/{limit}")
    Result<PageVo> findCustomerOrderPage(
            @PathVariable Long customerId,
            @PathVariable Long page,
            @PathVariable Long limit
    );

    @GetMapping("/order/info/findDriverOrderPage/{driverId}/{page}/{limit}")
    Result<PageVo> findDriverOrderPage(
            @PathVariable Long driverId,
            @PathVariable Long page,
            @PathVariable Long limit
    );

    @GetMapping("/order/info/getOrderBillInfo/{orderId}")
    Result<OrderBillVo> getOrderBillInfo(@PathVariable Long orderId);

    @GetMapping("/order/info/getOrderProfitsharing/{orderId}")
    Result<OrderProfitsharingVo> getOrderProfitsharing(@PathVariable Long orderId);

    @GetMapping("/order/info/sendOrderBillInfo/{orderId}/{driverId}")
    Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId,@PathVariable Long driverId);

    @GetMapping("/order/info/getOrderPayVo/{orderNo}/{customerId}")
    Result<OrderPayVo> getOrderPayVo(@PathVariable String orderNo, @PathVariable Long customerId);

    @GetMapping("/order/info/updateOrderPayStatus/{orderNo}")
    Result<Boolean> updateOrderPayStatus(@PathVariable String orderNo);

    @GetMapping("/order/info/getOrderRewardFee/{orderNo}")
    Result<OrderRewardVo> getOrderRewardFee(@PathVariable String orderNo);
}