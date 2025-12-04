// src/main/java/com/electrocyb/store/chat/ChatRequest.java
package com.electrocyb.store.chat;

import java.util.List;

public record ChatRequest(
        String message,
        List<MessageDto> history // opcional, para contexto
) {
}

record MessageDto(String role, String content) {}