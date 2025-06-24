package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {


    /**
     * 上传文件
     * @param file 文件
     * @param path 路径
     * @return 结果
     */
    CosUploadVo uploadFile(MultipartFile file, String path);
}
