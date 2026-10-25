package com.nexora.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    private String reply;

    private String intent;

    @Builder.Default
    private List<String> toolsExecuted = new ArrayList<>();

    private String provider; // "GEMINI" or "LOCAL_STUB"

    private boolean success;
}
