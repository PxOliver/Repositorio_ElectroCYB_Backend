package com.electrocyb.store.auth.dto;

public record AuthResponse(
        String token,
        UserDto user
) {}