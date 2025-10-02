package com.dayeyak.queue.auth;


import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Stream;

public enum UserRole {

    MASTER,
    SELLER,
    NORMAL,
    ;

    @JsonCreator
    public static UserRole of(String role) {
        return Stream.of(UserRole.values())
                .filter(userRole -> userRole.name().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(AuthExceptionType.INVALID_USER_ROLE.toString()));
    }
}