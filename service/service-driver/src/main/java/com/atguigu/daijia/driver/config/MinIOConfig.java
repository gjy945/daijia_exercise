package com.atguigu.daijia.driver.config;

import com.atguigu.daijia.common.util.MinIOUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class MinIOConfig {

    @Value("${minio.endpointUrl}")
    private String endpoint;
//    @Value("${minio.fileHost}")
//    private String fileHost;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secreKey}")
    private String secretKey;

    @Value("${minio.imgSize}")
    private Integer imgSize;
    @Value("${minio.fileSize}")
    private Integer fileSize;

    @Bean
    public void creatMinioClient() {
        new MinIOUtils(endpoint, bucketName, accessKey, secretKey, imgSize, fileSize);
    }
}

