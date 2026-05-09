package net.sfkao.ontobot.bus;

import reactor.core.publisher.Flux;

public interface MessageBus {
    void publish(BusMessage message);

    Flux<BusMessage> flux();
}