package net.sfkao.ontobot.bus;

import lombok.Builder;

import java.time.Instant;

/**
 * Represents a message that can be sent over the bus.
 * This record contains information about the source of the message, the author, the content, and the timestamp of when the message was created.
 *
 * @param sourceId  The identifier of the source that generated the message.
 * @param author    The author of the message.
 * @param content   The content of the message.
 * @param timestamp The timestamp indicating when the message was created.
 * @author Kao
 */
@Builder
public record BusMessage(
        String sourceId,
        String author,
        String content,
        Instant timestamp
) {
}