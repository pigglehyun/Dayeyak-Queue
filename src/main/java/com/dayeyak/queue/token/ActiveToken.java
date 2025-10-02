package com.dayeyak.queue.token;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Data
public class ActiveToken {
    String userId;
    String token;
    LocalDateTime expireAt;

    public ActiveToken(String userId, String token) {
        this.userId = userId;
        this.token = token;
        this.expireAt = LocalDateTime.now().plusMinutes(1);
    }

}
