package com.atguigu.daijia.driver.service.impl;

import cn.hutool.http.HttpStatus;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    private final CosFeignClient cosFeignClient;

    @Override
    public CosUploadVo uploadFile(MultipartFile file, String path) {
        // 远程调用
        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.upload(file,path);
        if (cosUploadVoResult.getCode() != HttpStatus.HTTP_OK){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return cosUploadVoResult.getData();
    }
}
