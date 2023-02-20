package io.github.ratelimiter.token;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 这是按流量设置的限流
 */
public class TokenBucket {
    private static final int DEFAULT_BUCKET_SIZE = 100;
    //一个桶的单位是一个字节
    private int everyTokenSize = 1;

    //瞬时最大流量
    private int maxFlowRate;
    //平均流量
    private int avgFlowRate;

    //队列缓存桶数量
    private ArrayBlockingQueue<Byte> tokenQueue = new ArrayBlockingQueue<Byte>(DEFAULT_BUCKET_SIZE);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean start = false;

    private final ReentrantLock lock = new ReentrantLock(true);

    private static final byte A_CHAR = 'a';

    public TokenBucket(){

    }

    public TokenBucket(int everyTokenSize, int maxFlowRate, int avgFlowRate){
        this.maxFlowRate = maxFlowRate;
        this.avgFlowRate = avgFlowRate;
        this.everyTokenSize = everyTokenSize;
    }

    public void addToken(Integer tokenNum){
        for(int i = 0; i < tokenNum; i++){
            tokenQueue.offer(Byte.valueOf(A_CHAR));
        }

    }

    public TokenBucket build(){
        start();
        return this;
    }

    /**
     * 获取足够的令牌个数
     * @return
     */
    public boolean getTokens(byte[] dataSize){
        int needTokenNum = dataSize.length / everyTokenSize + 1; //传输内容大小对应的桶的个数
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean result = needTokenNum <= tokenQueue.size();
            if(!result) {
                return false;
            }
            int tokenCount = 0;
            for(int i = 0; i < needTokenNum; i++) {
                Byte poll = tokenQueue.poll();
                if(poll != null) {
                    tokenCount ++;
                }
            }
            return tokenCount == needTokenNum;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 通过TokenProducer来给桶里加令牌
     */
    public void start(){
        //初始化桶队列大小
        if(maxFlowRate != 0){
            tokenQueue = new ArrayBlockingQueue<>(maxFlowRate);
        }
        //初始化令牌生产者
        TokenProducer tokenProducer = new TokenProducer(avgFlowRate, this);
        scheduledExecutorService.scheduleAtFixedRate(tokenProducer, 0, 1, TimeUnit.SECONDS);
        start = true;
    }

    public void stop(){
        start = false;
        scheduledExecutorService.shutdown();
    }

    public boolean isStart(){
        return start;
    }

    static class TokenProducer implements Runnable{
        private final int avgFlowRate;
        private final TokenBucket tokenBucket;

        public TokenProducer(int avgFlowRate, TokenBucket tokenBucket){
            this.avgFlowRate = avgFlowRate;
            this.tokenBucket = tokenBucket;
        }

        @Override
        public void run() {
            tokenBucket.addToken(avgFlowRate);
        }
    }

    public static void main(String[] args) throws Exception {
        tokenTest();
    }

    private static void tokenTest() throws Exception {
        TokenBucket tokenBucket = TokenBucketBuilder.newBuilder().avgFlowRate(512).maxFlowRate(1024).build();
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/Users/zhaojun/Downloads/demo/tokenTest.txt")));
        String data = "xxxx";
        for(int i = 0; i <= 1000; i++) {
            Random random = new Random();
            int i1 = random.nextInt();
            boolean tokens = tokenBucket.getTokens("xxxxxxxxxxx".getBytes());
//            TimeUnit.MILLISECONDS.sleep(1000);
            if(tokens) {
                bufferedWriter.write("token pass --- index:" + i1);
                System.out.println("token pass --- index:" + i1);
            } else {
                bufferedWriter.write("token rejuect --- index" + i1);
                System.out.println("token rejuect --- index" + i1);
            }
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        bufferedWriter.close();
    }
}
