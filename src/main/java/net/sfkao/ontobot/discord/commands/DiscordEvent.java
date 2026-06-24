package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.Event;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public interface DiscordEvent<T extends Event> {

    Class<T> getEventType();

    Mono<Void> execute(T event);

    Mono<Void> error(Throwable error);

}
