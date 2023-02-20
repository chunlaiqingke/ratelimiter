package io.github.ratelimiter.funnel;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Core， 单机实现
 * 漏斗模型的限流设计，和常规线程池和队列的异步实现方式，这个是同步的
 */
public class Funnel {

    /**
     * 容量
     */
    private int capacity;
    /**
     * 流速
     */
    private float leakingRate;
    /**
     * 剩余空间
     */
    private int leftQuota;
    /**
     * 上次流出的时间
     */
    private long leakingTs;

    ReentrantLock lock = new ReentrantLock(true);

    public Funnel(){

    }

    /**
     * count / timeWindow  是设置的qps
     * @param capacity
     * @param count
     * @param timeWindow, 单位是秒
     */
    public Funnel(int capacity, int count, int timeWindow) {
        this.capacity = capacity;
        timeWindow *= 1000;
        this.leakingRate = (float) count / timeWindow;
        leakingTs = System.currentTimeMillis();
    }

    /**
     * 根据上次水流动的时间，腾出已流出的空间
     */
    private void makeSpace(){
        long now = System.currentTimeMillis();
        long interval = now - leakingTs;
        int leaked = (int) (interval * leakingRate);
        if(leaked < 1) {
            return;
        }
        leftQuota += leaked;
        if(leftQuota > capacity) {
            leftQuota = capacity;
        }
        leakingTs = now;
    }

    /**
     * 每次请求之前计算上次请求之后漏出去多少水，以便为这次请求腾出空间
     * 只有有足够的剩余空间，才能放行
     * @param quota
     * @return
     */
    public boolean watering(int quota){
        try {
            lock.lock();
            makeSpace();
            int left = leftQuota - quota;
            if(left >= 0) {
                leftQuota = left;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
