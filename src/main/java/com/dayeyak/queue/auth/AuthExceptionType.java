package com.dayeyak.queue.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthExceptionType {

    INVALID_USER_ID("유효하지 않은 유저 ID입니다."),
    INVALID_USER_ROLE("유효하지 않은 유저 권한입니다."),
    REQUEST_ACCESS_DENIED("요청 접근 권한이 부족합니다.");

    private final String message;

    @Override
    public String toString() {
        return message;
    }
}