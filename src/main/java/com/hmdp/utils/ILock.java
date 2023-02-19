package com.hmdp.utils;

public interface ILock {

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 过期时间
     * @return true表示获取成功，false表示获取失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();
}
