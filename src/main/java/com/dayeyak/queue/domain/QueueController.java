package com.dayeyak.queue.domain;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/performance/{performanceId}/{userId}")
    public String queue(@PathVariable(name = "performanceId") Long pId, @PathVariable(name = "userId") Long uId) {
        queueService.addWaitingQueue(uId);
        return "eh?";
    }

    @GetMapping("/performance/{performanceId}/{userId}/{token}")
    public String refreshToken(@PathVariable(name = "performanceId") Long pId, @PathVariable(name = "userId") Long uId, @PathVariable(name = "token") String token) {
        queueService.addActiveQueue(String.valueOf(uId), token);
        return "eh?";
    }

    @GetMapping("/erase")
    public String queue() {
        queueService.eraseWaitingRedis();
        return "erase all";
    }


}
