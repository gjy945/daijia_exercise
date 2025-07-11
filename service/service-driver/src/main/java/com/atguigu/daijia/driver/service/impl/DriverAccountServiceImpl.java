package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.daijia.driver.mapper.DriverAccountDetailMapper;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.service.DriverAccountService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverAccountDetail;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverAccountServiceImpl extends ServiceImpl<DriverAccountMapper, DriverAccount> implements DriverAccountService {

    private final DriverAccountMapper driverAccountMapper;

    private final DriverAccountDetailMapper driverAccountDetailMapper;

    @Override
    public Boolean transfer(TransferForm transferForm) {
        // 1. 去重
        LambdaQueryWrapper<DriverAccountDetail> driverAccountDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        driverAccountDetailLambdaQueryWrapper.eq(DriverAccountDetail::getTradeNo,transferForm.getTradeNo());
        Long count = driverAccountDetailMapper.selectCount(driverAccountDetailLambdaQueryWrapper);
        if (count > 0){
            return true;
        }

        // 2. 添加奖励到账户表里面
        driverAccountMapper.add(transferForm.getDriverId(),transferForm.getAmount());

        // 3. 添加交易记录
        DriverAccountDetail driverAccountDetail = new DriverAccountDetail();
        BeanUtil.copyProperties(transferForm,driverAccountDetail);
        driverAccountDetailMapper.insert(driverAccountDetail);

        return true;
    }
}
