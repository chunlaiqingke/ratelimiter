package io.github.ratelimiter.slidewindow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 滑动窗口的单机实现
 */
public class SlideWindow {

    public volatile Map<String, List<Long>> map = new ConcurrentHashMap<>();

    public synchronized boolean isGo(String listId, int count, long timeWindow){
        // 获取当前时间
        long nowTime = System.currentTimeMillis();
        // 根据队列id，取出对应的限流队列，若没有则创建
        List<Long> list = map.computeIfAbsent(listId, k -> new LinkedList<>());
        // 如果队列还没满，则允许通过，并添加当前时间戳到队列开始位置
        if (list.size() < count) {
            list.add(0, nowTime);
            return true;
        }

        // 队列已满（达到限制次数），则获取队列中最早添加的时间戳
        Long farTime = list.get(count - 1);
        // 用当前时间戳 减去 最早添加的时间戳
        if (nowTime - farTime <= timeWindow) {
            // 若结果小于等于timeWindow，则说明在timeWindow内，通过的次数大于count
            // 不允许通过
            return false;
        } else {
            // 若结果大于timeWindow，则说明在timeWindow内，通过的次数小于等于count
            // 允许通过，并删除最早添加的时间戳，将当前时间添加到队列开始位置
            list.remove(count - 1);
            list.add(0, nowTime);
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SlideWindow slideWindow = new SlideWindow();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        while(true) {
            int count = 20;
            while(count-- >0){
                executorService.submit(() -> {
                    boolean go = slideWindow.isGo("127.0.0.1", 5, 1000);
                    System.out.println(Thread.currentThread().getName() + ":  " + go);
                });
            }
            Thread.sleep(4000);
            System.out.println("---------------------------------------");
        }
    }
}
