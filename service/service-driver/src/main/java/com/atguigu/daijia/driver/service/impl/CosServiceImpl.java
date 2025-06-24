package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.MinIOUtils;
import com.atguigu.daijia.driver.config.MinIOConfig;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.enums.ContentType;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    private final MinIOConfig minIOConfig;

    private final CiService ciService;

    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        // 文件后缀
        String fileType = Optional.ofNullable(file.getOriginalFilename())
                .map(s -> s.substring(file.getOriginalFilename().lastIndexOf(StrUtil.DOT)))
                .orElseThrow(() -> new GuiguException(ResultCodeEnum.DATA_ERROR));

        String uploadPath = "driver" + StrUtil.SLASH + path + StrUtil.SLASH + IdUtil.simpleUUID() + fileType;

        try {
            MinIOUtils.uploadFile(minIOConfig.getBucketName(),
                    uploadPath,
                    file.getInputStream(), ContentType.getContentType(fileType));
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 图片审核
        Boolean imageAuditing = ciService.imageAuditing(uploadPath);
        if (!imageAuditing){
            // 删除违规图片
            try {
                MinIOUtils.removeFile(minIOConfig.getBucketName(),uploadPath);
            } catch (Exception e) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            throw new GuiguException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
        }

        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        try {
            // 获取回显路径
            cosUploadVo.setShowUrl(MinIOUtils.getPresignedObjectUrl(
                    minIOConfig.getBucketName(),uploadPath
            ));
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return cosUploadVo;
    }
}
