package com.dayeyak.queue.queue;

import com.dayeyak.queue.token.ActiveToken;
import com.dayeyak.queue.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class QueueRepository {

    private final RedisTemplate<String, String> waitingRedisTemplate;
    private final RedisTemplate<String, ActiveToken> activeRedisTemplate;
    private final TokenProvider tokenProvider;


    public void save(String waitingToken) {
        double now = (System.currentTimeMillis() + Math.random());
        ZSetOperations<String, String> zSetOps = waitingRedisTemplate.opsForZSet();
        zSetOps.addIfAbsent("waiting", waitingToken, now); //userId 가 중복되지 않으면 추가
    }

    public void getWaitingQueue() {
        //토큰 만료되면 지우기
        Long num = waitingRedisTemplate.opsForZSet().size("waiting");
        Set<String> queue = waitingRedisTemplate.opsForZSet().range("waiting", 0, num);
        for (String token : queue) {
            Long rank = waitingRedisTemplate.opsForZSet().rank("waiting", token);
            String userId = tokenProvider.getUserIdFromWaitingToken(token);
            log.info("{}번 사용자 대기열 {}번째", userId, rank);
        }
    }

    public Long getSize() {
        return (waitingRedisTemplate.opsForZSet().size("waiting"));
    }


}
