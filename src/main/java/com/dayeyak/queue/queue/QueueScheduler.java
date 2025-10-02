package com.dayeyak.queue.queue;


import com.dayeyak.queue.token.ActiveToken;
import com.dayeyak.queue.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final RedisTemplate<String, String> waitingRedisTemplate;
    private final RedisTemplate<String, ActiveToken> activeRedisTemplate;
    private final TokenProvider tokenProvider;
    private final QueueService queueService;


    private final Long SIZE = 2L;


    //20초마다 활성 토큰 대기열 인원을 count하고,
    //N명의 사용자가 활성 토큰을 발급할 수 있다면 활성 토큰 발급
    @Scheduled(cron = "*/10 * * * * *")
    @Transactional
    public void updateWaitingQueue() {

        Long nowActive = activeRedisTemplate.opsForZSet().size("active");       //현재 활성 토큰을 가진 사람
        Long available = SIZE - nowActive;                                          //활성 토큰 발급 가능 사용자 수

        if (available > 0) {
            Set<String> queue = waitingRedisTemplate.opsForZSet().range("waiting", 0, available - 1);
            for (String token : queue) {
                String userId = tokenProvider.getUserIdFromWaitingToken(token);
                queueService.addActiveQueue(userId, token);
                queueService.eraseWaitingToken(token);
                log.info("***[ Scheduler ] 사용자 ( id: {} )  Active Token 발급 완료 ***", userId);
            }
        }
    }
}
