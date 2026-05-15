package net.sfkao.ontobot.bus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Sinks;

@Service
public class ReactiveMessageBus implements MessageBus {

    private final Sinks.Many<BusMessage> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void publish(BusMessage message) {
        sink.tryEmitNext(message);
    }

    @Override
    public Flux<BusMessage> flux() {
        return sink.asFlux();
    }

    @PostConstruct
    public void setupHooks() {
        Hooks.onErrorDropped(error -> {
            System.err.println("DROPPED REACTOR ERROR:");
            error.printStackTrace();
        });
    }
}