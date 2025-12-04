package com.electrocyb.store.auth.dto;

public record RegisterRequest(
        String fullName,
        String email,
        String password,
        String phone
) {}