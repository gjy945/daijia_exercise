package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.atguigu.daijia.common.constant.OtherConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.IpUtil;
import com.atguigu.daijia.common.util.MinIOUtils;
import com.atguigu.daijia.common.util.RandomName;
import com.atguigu.daijia.driver.config.MinIOConfig;
import com.atguigu.daijia.driver.config.WxConfigProperties;
import com.atguigu.daijia.driver.mapper.*;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.*;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

    private final WxMaService wxMaService;

    private final DriverSetMapper driverSetMapper;

    private final DriverAccountMapper driverAccountMapper;

    private final DriverLoginLogMapper driverLoginLogMapper;

    private final DriverInfoMapper driverInfoMapper;

    private final DriverFaceRecognitionMapper driverFaceRecognitionMapper;

    private final MinIOConfig minIOConfig;

    @Override
    public Long login(String code) {
        WxMaJscode2SessionResult sessionInfo;
        try {
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        String openid = sessionInfo.getOpenid();

        // 根据openId查询是否存在用户
        DriverInfo info = this.getOne(Wrappers.<DriverInfo>lambdaQuery().eq(DriverInfo::getWxOpenId, openid));

        // 如果存在直接登录
        if (!ObjectUtils.isEmpty(info)) {
            // 4 记录司机登录日志
            insertLoginLog(info.getId());

            return info.getId();
        }

        // 如果不存在，注册
        // 1 添加司机基本信息
        DriverInfo driverInfo = DriverInfo.builder()
                .wxOpenId(openid)
                .nickname(RandomName.randomName(true, 5))
                .avatarUrl(WxConfigProperties.defaultAvatar)
                .build();
        if (!this.save(driverInfo)) {
            throw new GuiguException(ResultCodeEnum.FAIL);
        }

        // 2 添加司机设置
        DriverSet driverSet = DriverSet.builder()
                .driverId(driverInfo.getId())
                .orderDistance(new BigDecimal(0)) // 默认无限制
                .acceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE))
                .isAutoAccept(0) // 0否 1是
                .build();
        if (driverSetMapper.insert(driverSet) <= 0) {
            throw new GuiguException(ResultCodeEnum.FAIL);
        }

        // 3 初始化司机账户信息
        DriverAccount driverAccount = DriverAccount.builder()
                .driverId(driverInfo.getId())
                .build();
        if (driverAccountMapper.insert(driverAccount) <= 0) {
            throw new GuiguException(ResultCodeEnum.FAIL);
        }

        // 4 记录司机登录日志
        insertLoginLog(driverInfo.getId());

        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        // 根据司机id获取司机信息
        DriverInfo driverInfo = Optional.ofNullable(this.getById(driverId)).orElseThrow(
                () -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        // 拷贝到vo
        DriverLoginVo driverLoginVo = BeanUtil.copyProperties(driverInfo, DriverLoginVo.class);

        // 是否建档人脸识别
        boolean isArchiveFace = StringUtils.hasText(driverInfo.getFaceModelId());
        driverLoginVo.setIsArchiveFace(isArchiveFace);

        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) throws Exception {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        DriverAuthInfoVo driverAuthInfoVo = BeanUtil.copyProperties(driverInfo, DriverAuthInfoVo.class);

        driverAuthInfoVo.setIdcardBackShowUrl(MinIOUtils.getPresignedObjectUrl(minIOConfig.getBucketName(), driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(MinIOUtils.getPresignedObjectUrl(minIOConfig.getBucketName(), driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(MinIOUtils.getPresignedObjectUrl(minIOConfig.getBucketName(), driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(MinIOUtils.getPresignedObjectUrl(minIOConfig.getBucketName(), driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(MinIOUtils.getPresignedObjectUrl(minIOConfig.getBucketName(), driverAuthInfoVo.getDriverLicenseHandUrl()));
        return null;
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        // 获取司机id
        Long driverId = updateDriverAuthInfoForm.getDriverId();

        // 修改操作
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(driverId);
        BeanUtil.copyProperties(updateDriverAuthInfoForm, driverInfo);

        return this.updateById(driverInfo);
    }

    @Override
    public Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverFaceModelForm.getDriverId());

        driverInfo.setFaceModelId(IdUtil.simpleUUID());
        driverInfoMapper.updateById(driverInfo);

        return true;
    }

    @Override
    public List<DriverSet> getDriverSet(Collection<Long> driverId) {
        LambdaQueryWrapper<DriverSet> queryWrapper = Wrappers.<DriverSet>lambdaQuery().in(DriverSet::getDriverId, driverId);
        return Optional.ofNullable(driverSetMapper.selectList(queryWrapper)).orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR));
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        DriverFaceRecognition driverFaceRecognition = driverFaceRecognitionMapper.selectOne(new LambdaQueryWrapper<DriverFaceRecognition>()
                .eq(DriverFaceRecognition::getDriverId, driverId)
                .eq(DriverFaceRecognition::getFaceDate, new DateTime().toString("yyyy-MM-dd")));

        return driverFaceRecognition != null;
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        // 1 照片比对


        // 2 如果照片比对成功，静态活体检测

        // 3 如果静态活体检测通过，添加数据到认证表中
        DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
        driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
        driverFaceRecognition.setFaceDate(new Date());
        int insert = driverFaceRecognitionMapper.insert(driverFaceRecognition);

        return insert > 0;
    }

    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {

        DriverSet driverSet = DriverSet.builder().serviceStatus(status).build();
        int rows = driverSetMapper.update(driverSet, new LambdaQueryWrapper<DriverSet>().eq(DriverSet::getDriverId, driverId));

        return rows > 0;
    }

    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        // 根据司机id获取基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        DriverInfoVo driverInfoVo = BeanUtil.copyProperties(driverInfo, DriverInfoVo.class);

        // 计算驾龄
        // 获取当前年
        int currentYear = new DateTime().getYear();
        int firstYear = new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear();

        driverInfoVo.setDriverLicenseAge(currentYear - firstYear);

        return driverInfoVo;
    }

    public void insertLoginLog(Long userId) {
        // 1 获取到request对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = sra.getRequest();

        DriverLoginLog driverLoginLog = DriverLoginLog.builder()
                .driverId(userId)
                .ipaddr(IpUtil.getIpAddress(request))
                .status(true)
                .msg(OtherConstant.WXLogin)
                .build();
        if (driverLoginLogMapper.insert(driverLoginLog) <= 0) {
            throw new GuiguException(ResultCodeEnum.FAIL);
        }

    }

    @Override
    public String getDriverOpenId(Long driverId) {
        return driverInfoMapper.selectById(driverId).getWxOpenId();
    }
}