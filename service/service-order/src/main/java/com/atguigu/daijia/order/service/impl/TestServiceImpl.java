package com.atguigu.daijia.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.order.service.TestService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author guojiye
 * @description
 * @date 2025/6/9
 **/
@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;


//    @Override
//    public synchronized void testLock() {
//        String value = stringRedisTemplate.opsForValue().get("num");
//
//        if (StrUtil.isBlank(value)){
//            return;
//        }
//
//        int num = Integer.parseInt(value);
//
//        stringRedisTemplate.opsForValue().set("num",String.valueOf(++num));
//    }


//    @Override
//    public void testLock() {
//        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent("这是一个锁", "这是一个锁");
//        if (!b) {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            testLock();
//        }
//        try {
//            String value = stringRedisTemplate.opsForValue().get("num");
//
//            if (StrUtil.isBlank(value)) {
//                return;
//            }
//
//            int num = Integer.parseInt(value);
//
//            stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            stringRedisTemplate.delete("这是一个锁");
//        }
//
//
//    }

    @Override
    public void testLock() {
        // 1 通过redisson创建锁对象
        RLock lock = redissonClient.getLock("lock1");

        // 2 尝试获取锁
        try {
            boolean b = lock.tryLock(30, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 3 编写业务代码

        String value = stringRedisTemplate.opsForValue().get("num");

        if (StrUtil.isBlank(value)) {
            return;
        }

        int num = Integer.parseInt(value);

        stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

        lock.unlock();
    }

}
