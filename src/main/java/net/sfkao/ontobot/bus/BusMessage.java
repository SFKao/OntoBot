package net.sfkao.ontobot.bus;

import java.time.Instant;
public record BusMessage(
        String sourceId,
        String author,
        String content,
        Instant timestamp
) {}