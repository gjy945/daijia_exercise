package com.atguigu.daijia.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.service.RabbitService;
import com.atguigu.daijia.config.WxPayV3Properties;
import com.atguigu.daijia.driver.client.DriverAccountFeignClient;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.enums.TradeType;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.order.OrderRewardVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.atguigu.daijia.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class WxPayServiceImpl implements WxPayService {

    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;

    private final PaymentInfoMapper paymentInfoMapper;

    private final WxPayV3Properties wxPayV3Properties;

    private final RabbitService rabbitService;

    private final OrderInfoFeignClient orderInfoFeignClient;

    private final DriverAccountFeignClient driverAccountFeignClient;


    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        try {
            // 添加支付记录到支付表里面
            // 判断： 如果存在订单支付记录，不添加
            PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                    .eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo()));
            if (paymentInfo == null) {
                paymentInfo = new PaymentInfo();
                BeanUtil.copyProperties(paymentInfoForm, paymentInfo);
                paymentInfo.setPaymentStatus(0);
                paymentInfoMapper.insert(paymentInfo);
            }

            // 创建一个微信支付使用对象
            JsapiServiceExtension jsapiServiceExtension = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

            // 创建一个request对象，封装微信支付所需要的参数
            PrepayRequest prepayRequest = new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(paymentInfoForm.getAmount().multiply(new BigDecimal(100)).intValue());
            prepayRequest.setAmount(amount);
            prepayRequest.setAppid(wxPayV3Properties.getAppId());
            prepayRequest.setMchid(wxPayV3Properties.getMerchantId());

            String content = paymentInfo.getContent();
            if (content.length() > 127) {
                content = content.substring(0, 127);
            }
            prepayRequest.setDescription(content);
            prepayRequest.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
            prepayRequest.setOutTradeNo(paymentInfo.getOrderNo());

            // 获取用户信息
            Payer payer = new Payer();
            payer.setOpenid(paymentInfoForm.getCustomerOpenId());
            prepayRequest.setPayer(payer);

            // 是否指定分账，不指定不能分账
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            prepayRequest.setSettleInfo(settleInfo);

            // 调用微信支付使用对象里面方法实现微信支付调用
            PrepayWithRequestPaymentResponse response = jsapiServiceExtension.prepayWithRequestPayment(prepayRequest);

            // 返回结果
            WxPrepayVo wxPrepayVo = BeanUtil.copyProperties(response, WxPrepayVo.class);
            wxPrepayVo.setTimeStamp(response.getTimeStamp());

            return wxPrepayVo;
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        // 1. 创建微信操作对象
        JsapiServiceExtension jsapiServiceExtension = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

        // 2. 封装查询支付状态需要参数
        QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
        queryOrderByOutTradeNoRequest.setMchid(wxPayV3Properties.getMerchantId());
        queryOrderByOutTradeNoRequest.setOutTradeNo(orderNo);

        // 3. 调用微信操作对象里面的方法实现查询操作
        Transaction transaction = jsapiServiceExtension.queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);

        // 4. 查询返回结果，根据结果判断
        if (transaction != null && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            // 5. 如果支付成功，调用其他方法实现支付后处理逻辑
            this.handlePayment(transaction);
            return true;
        }
        return false;
    }

    @SneakyThrows
    @Override
    public void wxnotify(HttpServletRequest request) {
        // 1， 回调通知的验签和解密
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        String requestBody = readData(request);

        // 2. 构造requestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        // 3 .初始化NotificationParser
        NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
        // 4. 以支付通知回调为例，验签解密并转换成Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS){
            // 处理支付业务
            this.handlePayment(transaction);
        }
    }

    // 5. 如果支付成功，调用其他方法实现支付后处理逻辑
    private void handlePayment(Transaction transaction) {
        // 1. 更新支付记录，把状态改为已经支付
        String orderNo = transaction.getOutTradeNo();

        // 根据订单编号查询支付记录
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo.getPaymentStatus() == 1){
            return;
        }

        paymentInfo.setPaymentStatus(1);
        paymentInfo.setOrderNo(transaction.getOutTradeNo());
        paymentInfo.setTransactionId(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        paymentInfoMapper.updateById(paymentInfo);

        // 2. 发送一个mq消息，传递相关参数 订单编号
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER,MqConst.ROUTING_PAY_SUCCESS,orderNo);

    }

    @Override
    public void handleOrder(String orderNo) {
        // 1. 远程调用：更新订单状态
        Boolean data = orderInfoFeignClient.updateOrderPayStatus(orderNo).getData();

        // 2. 获取系统奖励，打入司机账户
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        if (orderRewardVo != null && orderRewardVo.getRewardFee().doubleValue() > 0){
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeType.REWARD.getType());
            transferForm.setContent(TradeType.REWARD.getContent());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            data = driverAccountFeignClient.transfer(transferForm).getData();
        }

        // 3. TODO 其他

    }

    /**
     * 读取数据
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }
}
