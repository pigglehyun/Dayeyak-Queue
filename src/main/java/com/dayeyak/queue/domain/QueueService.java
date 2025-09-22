package com.dayeyak.queue.domain;

import com.dayeyak.queue.token.ActiveToken;
import com.dayeyak.queue.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, ActiveToken> objectRedisTemplate;


    private final TokenProvider tokenProvider;

    @Transactional
    public void addWaitingQueue(Long userId) {

        /*
        1. userID를 통한 JWT Token 생성
        2. zSetOps에 userIdm, JWTToken, 순번 저장
        * */
        long now = getCurrentTime();
        //1. userId를 통한 JWT Token 생성
        String waitingToken = tokenProvider.createWaitingToken(String.valueOf(userId));
        log.info("***[ #1 waiting token created. user : {}, token : {}]***", userId, waitingToken);

        //2. zSetOps에 userId, JWTToken, 순번 저장
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        zSetOps.addIfAbsent("waiting", waitingToken, now); //userId 가 중복되지 않으면 추가
        log.info("***[ #1 Waiting token saved in Redis , time : {}]***", now);

        // TODO :중복 처리 구현하기 (멀티컨슈머)

        getWaitingQueue();

        addActiveQueue(String.valueOf(userId), waitingToken);
    }

    @Transactional
    public void addActiveQueue(String userId, String waitingToken) {

        /*
        1. userId + UUID
        2. zSetOps에 userIdm, JWTToken, 순번 저장
        * */

        String token = String.valueOf(tokenProvider.createActiveToken(userId, waitingToken));
        ActiveToken activeToken = new ActiveToken(userId, token);
        long now = getCurrentTime();
        //1. userId를 통한 Token 생성
        log.info("***[ #2 Active token created. user : {}, token : {}]***", userId, activeToken);

        //2. zSetOps에 userId, JWTToken, 순번 저장
        ZSetOperations<String, ActiveToken> zSetOps = objectRedisTemplate.opsForZSet();
        zSetOps.addIfAbsent("active", activeToken, now); //userId 가 중복되지 않으면 추가
        log.info("***[ #2 Active token saved in Redis , time : {}]***", now);


        getActiveQueue();
    }

    public void publishNewWaitingToken() {

    }


    private void getWaitingQueue() {
        //토큰 만료되면 지우기
        Long num = stringRedisTemplate.opsForZSet().size("waiting");
        Set<String> queue = stringRedisTemplate.opsForZSet().range("waiting", 0, num);
        for (String token : queue) {
            Long rank = stringRedisTemplate.opsForZSet().rank("waiting", token);
            String userId = tokenProvider.getUserIdFromWaitingToken(token);
            log.info("{}번 사용자 대기열 {}번째", userId, rank);
        }
    }

    private void getActiveQueue() {
        //토큰 만료되면 지우기
        Long num = objectRedisTemplate.opsForZSet().size("active");
        Set<ActiveToken> queue = objectRedisTemplate.opsForZSet().range("active", 0, num);
        for (ActiveToken token : queue) {
            String userId = token.getUserId();
            Long rank = objectRedisTemplate.opsForZSet().rank("waiting", token);
            log.info("{}번 사용자 활성화 {}번째", userId, rank);
        }
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void eraseWaitingRedis() {
        stringRedisTemplate.opsForZSet().removeRange("waiting", 0, -1);
        stringRedisTemplate.opsForZSet().removeRange("active", 0, -1);

    }
}
