package io.github.ratelimiter.token;

public class TokenBucketBuilder {
    //一个桶的单位是一个字节
    private int everyTokenSize = 1;

    //瞬时最大流量
    private int maxFlowRate;
    //平均流量
    private int avgFlowRate;

    public static TokenBucketBuilder newBuilder(){
        return new TokenBucketBuilder();
    }

    public TokenBucketBuilder everyTokenSize(int everyTokenSize){
        this.everyTokenSize = everyTokenSize;
        return this;
    }

    public TokenBucketBuilder maxFlowRate(int maxFlowRate){
        this.maxFlowRate = maxFlowRate;
        return this;
    }

    public TokenBucketBuilder avgFlowRate(int avgFlowRate){
        this.avgFlowRate = avgFlowRate;
        return this;
    }

    public TokenBucket build(){
        return new TokenBucket(everyTokenSize, maxFlowRate, avgFlowRate);
    }
}
