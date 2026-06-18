package net.sfkao.ontobot.bus;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Sinks;

/**
 * ReactiveMessageBus is an implementation of the MessageBus interface that uses Project Reactor's Sinks to provide a reactive message bus.
 * It allows publishing messages and subscribing to a stream of messages in a non-blocking manner.
 *
 * @author Kao
 */
@Service
public class ReactiveMessageBus implements MessageBus {

    private final Sinks.Many<BusMessage> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void publish(final BusMessage message) {
        this.sink.tryEmitNext(message);
    }

    @Override
    public Flux<BusMessage> flux() {
        return this.sink.asFlux();
    }

    @PostConstruct
    public static void setupHooks() {
        Hooks.onErrorDropped(error -> {
            System.err.println("DROPPED REACTOR ERROR:");
            error.printStackTrace();
        });
    }
}