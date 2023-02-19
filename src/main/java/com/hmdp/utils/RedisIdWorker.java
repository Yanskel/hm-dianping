package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1672531200L;
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 自定义id生成策略
     *
     * @param keyPrefix id前缀
     * @return id
     */
    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        // 1.生成时间戳
        long currentSecond = now.toEpochSecond(ZoneOffset.UTC);
        // 2.计算时间戳(当前时间 - 设定开始时间)
        long timestamp = currentSecond - BEGIN_TIMESTAMP;

        // 3.生成序列号
        // 3.1.获取当前时间，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 3.2.自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return timestamp << COUNT_BITS | count;
    }
}
