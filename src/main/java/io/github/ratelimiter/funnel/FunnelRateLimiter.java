package io.github.ratelimiter.funnel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试类
 */
public class FunnelRateLimiter {
    /**
     * 多个令牌桶，按业务划分
     */
    private final Map<String, Funnel> funnelMap = new HashMap<>();

    public boolean isActionAllowed(String username, String action, int capacity, int allowQuota, int perSecond){
        String key = "funnel:" + action + ":" + username;
        if(!funnelMap.containsKey(key)){
            funnelMap.put(key, new Funnel(capacity, allowQuota, perSecond));
        }
        Funnel funnel = funnelMap.get(key);

        return funnel.watering(1);
    }

    public static void main(String[] args) throws InterruptedException {
        FunnelRateLimiter rateLimiter = new FunnelRateLimiter();
        int testAccessCount = 30000000;
        int capacity = 5;
        int allowQuota = 5;
        int perSecond = 1;
        int allowCount = 0;
        int denyCount = 0;
        for(int i = 0; i < testAccessCount ; i++) {
            boolean actionAllowed = rateLimiter.isActionAllowed("user", "dosomething", capacity, allowQuota, perSecond);
            if(actionAllowed) {
                allowCount++;
                System.out.println("时间" + new Date() + ", allowCount: " + allowCount);
            } else {
                denyCount ++;
            }
//            System.out.println("访问权限：" + actionAllowed + "， 拒绝数量：" + denyCount + "， 允许数量：" + allowCount);
//            Thread.sleep(50);
        }
    }
}
