package com.dayeyak.queue.queue;

import com.dayeyak.queue.token.ActiveToken;
import com.dayeyak.queue.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedisTemplate<String, String> waitingRedisTemplate;
    private final RedisTemplate<String, ActiveToken> activeRedisTemplate;
    private final KafkaTemplate<String, ActiveToken> kafkaTemplate;
    private final QueueRepository queueRepository;
    private final TokenProvider tokenProvider;


    @Transactional
    public void addWaitingQueue(Long userId) {

        //1. 요청 시간과 userId 기반 대기 토큰 생성
        double now = getCurrentTime();
        String waitingToken = tokenProvider.createWaitingToken(String.valueOf(userId));
        log.info("***[ #1 waiting token created. user : {}, token : {}]***", userId, waitingToken);

        //2. Redis에 요청 시간을 기준으로 대기 토큰 저장
        ZSetOperations<String, String> zSetOps = waitingRedisTemplate.opsForZSet();
        zSetOps.addIfAbsent("waiting", waitingToken, now); //userId 가 중복되지 않으면 추가
        log.info("***[ #1 Waiting token saved in Redis , time : {}]***", now);

        // TODO :중복 처리 구현하기 (멀티컨슈머)

    }

    @Transactional
    public void addActiveQueue(String userId, String waitingToken) {

        /*
        1. userId + UUID
        2. zSetOps에 userIdm, JWTToken, 순번 저장
        * */
        //1. userId를 기반으로 랜덤 UUID 활성 토큰 생성
        double now = (getCurrentTime() + Math.random());
        String token = String.valueOf(tokenProvider.createActiveToken(userId, waitingToken));

        if (token == null) {
            eraseWaitingToken(waitingToken);
        } else {
            ActiveToken activeToken = new ActiveToken(userId, token);
            log.info("***[ #2 Active token created. user : {}, token : {}, expire at : {}]***", userId, activeToken.getToken(), activeToken.getExpireAt());

            //2. zSetOps에 userId, JWTToken, 순번 저장
            ZSetOperations<String, ActiveToken> zSetOps = activeRedisTemplate.opsForZSet();
            zSetOps.addIfAbsent("active", activeToken, now); //userId 가 중복되지 않으면 추가
            log.info("***[ #2 Active token saved in Redis , time : {}]***", now);

            //3. 예매로 해당 유저 보내주기
            produceActiveUser(activeToken);
        }
    }

//    @Transactional
//    public void reissueWaitingToken(String userId, String oldToken) {
//        if(tokenProvider.isValid(oldToken)){
//            String newToken = tokenProvider.createWaitingToken(userId);
//            Double now = waitingRedisTemplate.opsForZSet().score("waiting", oldToken);
//            waitingRedisTemplate.opsForZSet().remove("waiting",oldToken);
//            waitingRedisTemplate.opsForZSet().addIfAbsent("waiting",newToken,now);
//        }
//    }
//
//    private void getWaitingQueue() {
//        //토큰 만료되면 지우기
//        Long num = waitingRedisTemplate.opsForZSet().size("waiting");
//        Set<String> queue = waitingRedisTemplate.opsForZSet().range("waiting", 0, num);
//        for (String token : queue) {
//            Long rank = waitingRedisTemplate.opsForZSet().rank("waiting", token);
//            String userId = tokenProvider.getUserIdFromWaitingToken(token);
//            log.info("{}번 사용자 대기열 {}번째", userId, rank);
//        }
//    }
//
//    private void getActiveQueue() {
//        //토큰 만료되면 지우기
//        Long num = activeRedisTemplate.opsForZSet().size("active");
//        Set<ActiveToken> queue = activeRedisTemplate.opsForZSet().range("active", 0, num);
//        for (ActiveToken token : queue) {
//            String userId = token.getUserId();
//            Long rank = activeRedisTemplate.opsForZSet().rank("active", token);
//            log.info("{}번 사용자 활성화 {}번째", userId, rank);
//        }
//    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    @Transactional
    public void eraseWaitingToken(String token) {
        waitingRedisTemplate.opsForZSet().remove("waiting", token);
    }

    @Transactional
    public void eraseActiveToken(ActiveToken token) {
        activeRedisTemplate.opsForZSet().remove("active", token);
    }

    @Transactional
    public void eraseWaitingRedis() {
        waitingRedisTemplate.opsForZSet().removeRange("waiting", 0, -1);
        activeRedisTemplate.opsForZSet().removeRange("active", 0, -1);
    }


    @Transactional
    public void produceActiveUser(ActiveToken activeToken) {
        String topic = "active-token-start";
        String key = "1";
        kafkaTemplate.send(topic, key, activeToken);
    }

    @Transactional
    public void produceActiveUserInBooking(ActiveToken activeToken) {
        String topic = "active-token-done";
        String key = "1";
        kafkaTemplate.send(topic, key, activeToken);
    }

    @KafkaListener(groupId = "queue", topics = "active-token-done")
    @Transactional
    public void consumeActiveUser(ActiveToken activeToken) {
        String userId = activeToken.getUserId();
        eraseActiveToken(activeToken);
        log.info("****[ {} Active Token 삭제 완료]****", userId);
    }


}
