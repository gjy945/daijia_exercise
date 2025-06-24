package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.common.error.WxErrorException;

public interface CustomerInfoService extends IService<CustomerInfo> {

    /**
     * 微信小程序登录接口
     * @param code 授权码
     * @return
     */
    Long login(String code);

    /**
     * 获取客户登录信息
     * @param customerId 客户id
     * @return
     */
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 获取客户openId
     * @param customerId
     * @return
     */
    String getCustomerOpenId(Long customerId);
}
