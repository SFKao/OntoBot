package net.sfkao.ontobot.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
@Component
@RequiredArgsConstructor
public class DiscordChannelOntoAdapter {

    public static final String SOURCE_ID =
            "Discord";

    private static final Snowflake CHANNEL_ID =
            Snowflake.of("1492043447639740546");

    private final GatewayDiscordClient client;
    private final MessageBus bus;

    @PostConstruct
    public void init() {

        listenDiscord();

        listenBus();
    }


    private void listenDiscord() {

        client.on(MessageCreateEvent.class)
                .filter(event ->
                        event.getMessage()
                                .getChannelId()
                                .equals(CHANNEL_ID))
                .filter(event ->
                        !event.getMessage()
                                .getAuthor()
                                .map(User::isBot)
                                .orElse(false))
                .subscribe(event -> {

                    String content =
                            event.getMessage().getAuthor().map(user -> user.asMember(event.getGuildId().get()).block().getDisplayName()).orElse("") +": " +
                            event.getMessage()
                                    .getContent();

                    bus.publish(new BusMessage(
                            SOURCE_ID,
                            content,
                            Instant.now()
                    ));
                });
    }

    private void listenBus() {

        bus.flux()
                .filter(msg ->
                        !msg.sourceId()
                                .equals(SOURCE_ID))
                .flatMap(this::sendToDiscord)
                .subscribe();
    }

    private Mono<Void> sendToDiscord(
            BusMessage message
    ) {

        return client.getChannelById(CHANNEL_ID)
                .cast(MessageChannel.class)
                .flatMap(channel ->
                        channel.createMessage(
                                "[" + message.sourceId() + "] "
                                        + message.content()
                        ))
                .then();
    }
}
