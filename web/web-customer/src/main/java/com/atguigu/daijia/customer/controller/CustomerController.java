package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.constant.OtherConstant;
import com.atguigu.daijia.common.login.Login;
import com.atguigu.daijia.common.login.LoginAspect;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerService.login(code));
    }

    @Operation(summary = "获取客户端登录信息")
    @Login
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(HttpServletRequest request) {
        return Result.ok(customerService.getCustomerLoginInfo());

    }

    @Operation(summary = "更新用户微信手机号")
    @Login
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm){
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(true);
    }

    @Operation(summary = "获取客户openId")
    @GetMapping("/getCustomerOpenId")
    public Result<String> getCustomerOpenId(){
        return Result.ok(customerService.getCustomerOpenId(AuthContextHolder.getUserId()));
    }

}

