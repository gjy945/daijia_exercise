package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {

    /**
     * 微信小程序授权登录
     * @param code 授权码
     * @return
     */
    String login(String code);


    /**
     * 获取客户端登录信息
     * @return 登录信息
     */
    CustomerLoginVo getCustomerLoginInfo();

    /**
     * 获取客户openId
     * @param userId
     * @return
     */
    String getCustomerOpenId(Long userId);
}
