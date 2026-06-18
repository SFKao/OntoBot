package net.sfkao.ontobot.bus;

import reactor.core.publisher.Flux;

/**
 * Interface representing a message bus that allows publishing and subscribing to messages.
 * Implementations of this interface should provide mechanisms for publishing messages and subscribing to a stream of messages.
 *
 * @author Kao
 */
public interface MessageBus {

    /**
     * Publishes a message to the message bus.
     *
     * @param message the message to be published
     */
    void publish(BusMessage message);

    /**
     * Returns a Flux that represents a stream of messages published to the message bus.
     * Subscribers can listen to this Flux to receive messages in a reactive manner.
     *
     * @return a Flux of BusMessage representing the stream of messages
     */
    Flux<BusMessage> flux();
}