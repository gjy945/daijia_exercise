package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import com.atguigu.daijia.common.constant.OtherConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.IpUtil;
import com.atguigu.daijia.common.util.RandomName;
import com.atguigu.daijia.customer.config.WxConfigProperties;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    private final WxMaService wxMaService;

    private final CustomerInfoMapper customerInfoMapper;

    private final CustomerLoginLogMapper customerLoginLogMapper;

    protected final HttpServletRequest request;

    @Override
    public Long login(String code) {
        // 1 获取到code值，使用微信工具包中的对象，获取微信的唯一表示openId
        WxMaJscode2SessionResult sessionInfo = null;
        try {
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }
        if (ObjectUtils.isEmpty(sessionInfo) || !StringUtils.hasText(sessionInfo.getOpenid())) {
            throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }

        // 2 根据openId查询数据库表，判断是否是第一次登录
        String openid = sessionInfo.getOpenid();
        LambdaQueryWrapper<CustomerInfo> wrapper = Wrappers.<CustomerInfo>lambdaQuery().eq(CustomerInfo::getWxOpenId, openid);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);

        // 3 如果第一次登录，添加用户信息到用户表中
        if (ObjectUtils.isEmpty(customerInfo)) {
            CustomerInfo insertInfo = CustomerInfo.builder()
                    .wxOpenId(openid)
                    .nickname(RandomName.randomName(true, new Random().nextInt(4) + 2))
                    .avatarUrl(WxConfigProperties.defaultAvatar)
                    .build();
            if (!(customerInfoMapper.insert(insertInfo) > 0)) {
                throw new GuiguException(ResultCodeEnum.FAIL);
            }
            return insertInfo.getId();
        }

        // 4 记录登录的日志信息
        CustomerLoginLog customerLoginLog = CustomerLoginLog.builder()
                .customerId(customerInfo.getId())
                .ipaddr(IpUtil.getIpAddress(request))
                .status(true)
                .msg(OtherConstant.WXLogin)
                .build();
        if (!(customerLoginLogMapper.insert(customerLoginLog) > 0)) {
            throw new GuiguException(ResultCodeEnum.FAIL);
        }

        // 5 返回用户id
        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        // 1 根据用户id查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        if (ObjectUtils.isEmpty(customerInfo)){
            throw new GuiguException(ResultCodeEnum.CUSTOMER_NOT_EXIST);
        }

        // 2 封装到CustomerLoginVo
        CustomerLoginVo customerLoginVo = BeanUtil.copyProperties(customerInfo, CustomerLoginVo.class);
//        if (StrUtil.isBlank(customerInfo.getPhone())){
//            customerLoginVo.setIsBindPhone(Boolean.FALSE);
//        }else {
//            customerLoginVo.setIsBindPhone(Boolean.TRUE);
//        }
        boolean hasText = StringUtils.hasText(customerInfo.getPhone());
        customerLoginVo.setIsBindPhone(hasText);

        // 3 返回CustomerLoginVo
        return customerLoginVo;
    }

    @Override
    public String getCustomerOpenId(Long customerId) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        return customerInfo.getWxOpenId();
    }
}
