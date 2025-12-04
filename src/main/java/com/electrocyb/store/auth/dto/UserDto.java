package com.electrocyb.store.auth.dto;

public record UserDto(
        Long id,
        String fullName,
        String email,
        String phone,
        String role
) {}