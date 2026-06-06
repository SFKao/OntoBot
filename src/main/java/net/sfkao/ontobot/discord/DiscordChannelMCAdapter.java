package net.sfkao.ontobot.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DiscordChannelMCAdapter {

    Map<String, String> DICTTIONARY_MC_TO_USERNAME =
            Map.of(
                    "SFKao", "Kao",
                    "ElshOwO", "Elsho"
            );

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

        this.client.getChannelById(DiscordChannelMCAdapter.CHANNEL_ID)
                .cast(MessageChannel.class)
                .doOnNext(ch -> {

                    this.channel = ch;

                    System.out.println(
                            "Discord channel ready: "
                                    + DiscordChannelMCAdapter.CHANNEL_ID.asString()
                    );

                    this.listenDiscord();

                    this.listenBus();
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

        this.client.on(MessageCreateEvent.class)
                .filter(event ->
                        event.getMessage()
                                .getChannelId()
                                .equals(DiscordChannelMCAdapter.CHANNEL_ID))
                .filter(event ->
                        !event.getMessage()
                                .getContent()
                                .startsWith("[SYS]"))
                .filter(event ->
                        event.getMessage()
                                .getAuthor()
                                .map(user ->
                                        !user.getId()
                                                .equals(DiscordChannelMCAdapter.SELF_BOT_ID))
                                .orElse(true))
                .subscribe(event -> {

                    this.bus.publish(new BusMessage(
                            DiscordChannelMCAdapter.SOURCE_ID,
                            this.DICTTIONARY_MC_TO_USERNAME.getOrDefault(event.getMessage().getUserData().username(), event.getMessage().getUserData().username()),
                            event.getMessage()
                                    .getContent(),
                            Instant.now()
                    ));
                });
    }

    private void listenBus() {

        this.bus.flux()
                .filter(msg ->
                        !msg.sourceId()
                                .equals(DiscordChannelMCAdapter.SOURCE_ID))
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
            final BusMessage message
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