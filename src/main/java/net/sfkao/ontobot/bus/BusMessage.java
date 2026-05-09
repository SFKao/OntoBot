package net.sfkao.ontobot.bus;

import java.time.Instant;
import java.util.UUID;

public record BusMessage(
        String sourceId,
        String content,
        Instant timestamp
) {}