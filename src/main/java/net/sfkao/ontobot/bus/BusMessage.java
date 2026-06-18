package net.sfkao.ontobot.bus;

import lombok.Builder;

import java.time.Instant;

@Builder
public record BusMessage(
        String sourceId,
        String author,
        String content,
        Instant timestamp
) {
}