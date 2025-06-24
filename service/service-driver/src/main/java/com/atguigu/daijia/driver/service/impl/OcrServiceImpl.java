package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.MinIOUtils;
import com.atguigu.daijia.driver.config.MinIOConfig;
import com.atguigu.daijia.driver.service.OcrService;
import com.atguigu.daijia.model.enums.ContentType;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrServiceImpl implements OcrService {

    private final MinIOConfig minIOConfig;

    @Override
    public IdCardOcrVo idCardOcr(MultipartFile file) throws Exception {

        // 文件后缀
        String fileType = Optional.ofNullable(file.getOriginalFilename())
                .map(s -> s.substring(file.getOriginalFilename().lastIndexOf(StrUtil.DOT)))
                .orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        String uploadPath = "driver" + StrUtil.SLASH + "idCard" + StrUtil.SLASH + IdUtil.simpleUUID() + fileType;

        try {
            MinIOUtils.uploadFile(minIOConfig.getBucketName(),
                    uploadPath,
                    file.getInputStream(), ContentType.getContentType(fileType));
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 定义日期格式
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        // 要转换的字符串
        String dateInString = "2004.06.13";
        // 解析字符串为Date对象
        Date date = formatter.parse(dateInString);

        IdCardOcrVo idCardOcrVo = new IdCardOcrVo();
        idCardOcrVo.setName("张三");
        idCardOcrVo.setGender("男");
        idCardOcrVo.setBirthday(date);
        idCardOcrVo.setIdcardNo("152630200505050505");
        idCardOcrVo.setIdcardAddress("天津市XXXX");
        idCardOcrVo.setIdcardExpire(date);
        idCardOcrVo.setIdcardFrontUrl(uploadPath);
        idCardOcrVo.setIdcardFrontShowUrl(MinIOUtils.getPresignedObjectUrl(
                minIOConfig.getBucketName(), uploadPath
        ));
        idCardOcrVo.setIdcardBackUrl(uploadPath);
        idCardOcrVo.setIdcardBackShowUrl(MinIOUtils.getPresignedObjectUrl(
                minIOConfig.getBucketName(), uploadPath
        ));

        return idCardOcrVo;
    }

    @Override
    public DriverLicenseOcrVo driverLincenseOcr(MultipartFile file) throws Exception {

        // 文件后缀
        String fileType = Optional.ofNullable(file.getOriginalFilename())
                .map(s -> s.substring(file.getOriginalFilename().lastIndexOf(StrUtil.DOT)))
                .orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        String uploadPath = "driver" + StrUtil.SLASH + "driverLicense" + StrUtil.SLASH + IdUtil.simpleUUID() + fileType;

        try {
            MinIOUtils.uploadFile(minIOConfig.getBucketName(),
                    uploadPath,
                    file.getInputStream(), ContentType.getContentType(fileType));
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 定义日期格式
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        // 要转换的字符串
        String dateInString = "2004.06.13";
        // 解析字符串为Date对象
        Date date = formatter.parse(dateInString);

        DriverLicenseOcrVo driverLicenseOcrVo = new DriverLicenseOcrVo();
        driverLicenseOcrVo.setName("张三");
        driverLicenseOcrVo.setDriverLicenseClazz("C1");
        driverLicenseOcrVo.setDriverLicenseNo("123123123");
        driverLicenseOcrVo.setDriverLicenseExpire(date);
        driverLicenseOcrVo.setDriverLicenseIssueDate(date);
        driverLicenseOcrVo.setDriverLicenseFrontUrl(uploadPath);
        driverLicenseOcrVo.setDriverLicenseFrontShowUrl(MinIOUtils.getPresignedObjectUrl(
                minIOConfig.getBucketName(), uploadPath
        ));
        driverLicenseOcrVo.setDriverLicenseBackUrl(uploadPath);
        driverLicenseOcrVo.setDriverLicenseBackShowUrl(MinIOUtils.getPresignedObjectUrl(
                minIOConfig.getBucketName(), uploadPath
        ));

        return driverLicenseOcrVo;
    }
}
