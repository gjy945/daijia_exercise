package com.atguigu.daijia.payment.service;

import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {


    /**
     * 创建微信支付
     * @param paymentInfoForm
     * @return
     */
    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    /**
     * 支付状态查询
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 微信支付成功之后进行的回调
     * @param request
     */
    void wxnotify(HttpServletRequest request);

    /**
     * 支付成功后续处理
     * @param orderNo
     */
    void handleOrder(String orderNo);
}
