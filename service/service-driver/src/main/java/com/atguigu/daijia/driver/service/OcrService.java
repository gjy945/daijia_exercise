package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;

public interface OcrService {


    /**
     * 身份证识别
     * @param file
     * @return
     */
    IdCardOcrVo idCardOcr(MultipartFile file) throws Exception;


    /**
     * 驾驶证识别
     * @param file
     * @return
     */
    DriverLicenseOcrVo driverLincenseOcr(MultipartFile file) throws Exception;
}
