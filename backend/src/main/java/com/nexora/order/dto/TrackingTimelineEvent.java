package com.nexora.order.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingTimelineEvent {
    private String status;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private boolean completed;
    private boolean current;
}
