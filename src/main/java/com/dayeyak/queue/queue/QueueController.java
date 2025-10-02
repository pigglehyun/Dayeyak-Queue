package com.dayeyak.queue.queue;


import com.dayeyak.queue.auth.Passport;
import com.dayeyak.queue.auth.PassportHolder;
import com.dayeyak.queue.token.ActiveToken;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    @RateLimiter(name = "queueIntake")
    @Retry(name = "queueRetry")
    @GetMapping("/performance/{performanceId}/{userId}")
    public String queue(@PathVariable(name = "performanceId") Long pId, @PathVariable(name = "userId") Long uId) {
        queueService.addWaitingQueue(uId);
        return "waiting token";
    }

    @PostMapping("/bookings/orchestration")
    public void queue(
            @RequestBody RequestDto requestDto, @PassportHolder Passport passport
    ) {
        Long userId = requestDto.userId();
        queueService.addWaitingQueue(userId);
    }

    @PostMapping("/bookings/orchestration/done")
    public void sendActiveQueueDone(
            @RequestBody ActiveToken activeToken
    ) {
        queueService.produceActiveUserInBooking(activeToken);
    }

    @GetMapping("/erase")
    public String queue() {
        queueService.eraseWaitingRedis();
        return "erase all";
    }

}
