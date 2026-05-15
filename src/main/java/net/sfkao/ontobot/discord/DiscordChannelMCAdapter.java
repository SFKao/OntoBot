package net.sfkao.ontobot.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
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
public class DiscordChannelMCAdapter {

    public static final String SOURCE_ID =
            "MC";

    private static final Snowflake CHANNEL_ID =
            Snowflake.of("1480982745214877777");

    private MessageChannel channel;

    private static final Snowflake SELF_BOT_ID =
            Snowflake.of("1491855143594102845");

    private final GatewayDiscordClient client;
    private final MessageBus bus;

    @PostConstruct
    public void init() {

        client.getChannelById(CHANNEL_ID)
                .cast(MessageChannel.class)
                .doOnNext(ch -> {

                    this.channel = ch;

                    System.out.println(
                            "Discord channel ready: "
                                    + CHANNEL_ID.asString()
                    );

                    listenDiscord();

                    listenBus();
                })
                .subscribe(
                        null,
                        error -> {
                            System.err.println("Reactor error:");
                            error.printStackTrace();
                        }
                );
    }

    private void listenDiscord() {

        client.on(MessageCreateEvent.class)
                .filter(event ->
                        event.getMessage()
                                .getChannelId()
                                .equals(CHANNEL_ID))
                .filter(event ->
                        event.getMessage()
                                .getAuthor()
                                .map(user ->
                                        !user.getId()
                                                .equals(SELF_BOT_ID))
                                .orElse(true))
                .subscribe(event -> {

                    bus.publish(new BusMessage(
                            SOURCE_ID,
                            event.getMessage().getUserData().username(),
                            event.getMessage()
                                    .getContent(),
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
                .subscribe(
                        null,
                        error -> {
                            System.err.println(
                                    "Discord send failed"
                            );

                            error.printStackTrace();
                        }
                );
    }

    private Mono<Void> sendToDiscord(
            BusMessage message
    ) {

        if (this.channel == null) {
            return Mono.empty();
        }

        return this.channel.createMessage(
                        "**" + message.author() + ":** "
                                + message.content()
                )
                .then();
    }
}