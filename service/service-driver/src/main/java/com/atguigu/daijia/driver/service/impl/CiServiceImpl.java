package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import org.springframework.stereotype.Service;

@Service
public class CiServiceImpl implements CiService {


    @Override
    public Boolean imageAuditing(String path) {
        return true;
    }

    @Override
    public TextAuditingVo textAuditing(String content) {
        TextAuditingVo textAuditingVo = new TextAuditingVo();

        if (StrUtil.isBlank(content)){
            textAuditingVo.setResult("0");
            return textAuditingVo;
        }

        textAuditingVo.setResult("0");
        return textAuditingVo;
    }
}
