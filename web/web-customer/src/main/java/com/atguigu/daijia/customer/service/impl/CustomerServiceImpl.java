package com.atguigu.daijia.customer.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.el.parser.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    // 注入远程调用接口
    private final CustomerInfoFeignClient customerInfoFeignClient;

    private final RedisTemplate redisTemplate;

    @Override
    public String login(String code) {
        // 1 拿着code进行远程调用，返回用户id
        Result<Long> login = customerInfoFeignClient.login(code);

        // 2 判断如果返回失败了，返回错误的提示（状态码不为200统一失败）
        if (login.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 3 获取远程调用返回用户id
        Long userId = login.getData();

        // 4 判断返回用户id是否为空，如果为空，返回错误提示
        if (userId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 5 生成token字符串
        String token = IdUtil.simpleUUID();

        // 6 把用户id放到redis，设置过期时间
        redisTemplate.opsForValue().set(
                RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                userId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 7 返回token
        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo() {
        Long customerId = AuthContextHolder.getUserId();

        if (customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 4 根据用户id进行远程调用，得到用户信息
        Result<CustomerLoginVo> customerLoginInfo = customerInfoFeignClient.getCustomerLoginInfo(customerId);
        if (customerLoginInfo.getCode() != 200 || ObjectUtils.isEmpty(customerLoginInfo.getData())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 5 返回用户信息
        return customerLoginInfo.getData();
    }

    @Override
    public String getCustomerOpenId(Long userId) {
        return customerInfoFeignClient.getCustomerOpenId(userId).getData();
    }
}
