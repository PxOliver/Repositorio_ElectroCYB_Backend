package com.electrocyb.store.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
