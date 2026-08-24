package com.blog.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * 线程安全的雪花 ID 生成器
 * 说明：MyBatis-Plus 默认生成器在多线程并发插入（消费者线程 + 请求线程同一毫秒）时存在 ID 冲突风险
 * （duplicate PRIMARY KEY，见阶段5排障记录），这里用 synchronized 保证唯一性。
 *
 * @author Liangkunrui
 */
@Component
public class ThreadSafeIdentifierGenerator implements IdentifierGenerator {

    private static final long TWEPOCH = 1700000000000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId = 1L;
    private final long datacenterId = 1L;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    @Override
    public synchronized Number nextId(Object entity) {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨：沿用上一毫秒时间戳，保证单调递增
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - TWEPOCH) << (WORKER_ID_BITS + DATACENTER_ID_BITS + SEQUENCE_BITS))
                | (datacenterId << (WORKER_ID_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
