package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.order.mapper.OrderMonitorMapper;
import com.atguigu.daijia.order.repository.OrderMonitorRecordRepository;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements OrderMonitorService {

    @Autowired
    private OrderMonitorRecordRepository orderMonitorRecordRepository;

    @Override
    public Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord) {
        orderMonitorRecordRepository.save(orderMonitorRecord);
        return !ObjectUtils.isEmpty(orderMonitorRecord);
    }

    @Override
    public Long saveOrderMonitor(OrderMonitor orderMonitor) {
        this.save(orderMonitor);
        return orderMonitor.getId();
    }

    @Override
    public OrderMonitor getOrderMonitor(Long orderId) {
        return this.getOne(new LambdaQueryWrapper<OrderMonitor>().eq(OrderMonitor::getOrderId, orderId));
    }

    @Override
    public Boolean updateOrderMonitor(OrderMonitor orderMonitor) {
        return this.updateById(orderMonitor);
    }
}
