package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.Login;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {


    private final OrderService orderService;


    @Operation(summary = "查询订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "查询司机新订单数据")
    @Login
    @GetMapping("/findNewOrderQueueData")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(){
        return Result.ok(orderService.findNewOrderQueueData(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "司机端查找当前订单")
    @Login
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchDriverCurrentOrder(driverId));
    }

    @Operation(summary = "司机抢单")
    @Login
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId){
        return Result.ok(orderService.robNewOrder(AuthContextHolder.getUserId(),orderId));
    }

    @Operation(summary = "获取订单信息")
    @Login
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId){
        return Result.ok(orderService.getOrderInfo(AuthContextHolder.getUserId(),orderId));
    }

    @Operation(summary = "计算最佳驾驶路线")
    @Login
    @GetMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm){
        return Result.ok(orderService.calcuateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "司机到达起始点")
    @Login
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId){
        return Result.ok(orderService.driverArriveStartLocation(orderId,AuthContextHolder.getUserId()));
    }

    @Operation(summary = "更新代驾车辆信息")
    @Login
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm){
        updateOrderCartForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(orderService.updateOrderCart(updateOrderCartForm));
    }

    @Operation(summary = "开始代驾服务")
    @Login
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm){
        startDriveForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(orderService.startDrive(startDriveForm));
    }

    @Operation(summary = "结束代驾服务更新订单账单")
    @Login
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm){
        orderFeeForm.setDriverId(AuthContextHolder.getUserId());
//        return Result.ok(orderService.endDrive(orderFeeForm));
        return Result.ok(orderService.endDriveThread(orderFeeForm));
    }

    @Operation(summary = "获取司机订单分页列表")
    @Login
    @GetMapping("/findDriverOrderPage/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(
            @Parameter(name = "page",description = "当前页码",required = true)
            @PathVariable Long page,
            @Parameter(name = "limit",description = "每页记录数",required = true)
            @PathVariable Long limit
    ){
        PageVo pageVo = orderService.findDriverOrderPage(AuthContextHolder.getUserId(),page,limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "发送账单信息")
    @Login
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId){
        return Result.ok(orderService.sendOrderBillInfo(orderId,AuthContextHolder.getUserId()));
    }




}

