package com.dayeyak.queue.auth;

public record Passport(
        Long userId,

        UserRole role
) {
}