package net.sfkao.ontobot.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * DiscordChannelMCAdapter is a component that listens to a specific Discord channel and relays messages to a message bus.
 * It also listens to the message bus for messages from other sources and sends them to the Discord channel.
 *
 * @author Kao
 */
@Component
@RequiredArgsConstructor
public class DiscordChannelMCAdapter {

    /**
     * A mapping of Minecraft usernames to their corresponding Discord usernames.
     */
    Map<String, String> DICTTIONARY_MC_TO_USERNAME =
            Map.of(
                    "SFKao", "Kao",
                    "ElshOwO", "Elsho"
            );

    public static final String SOURCE_ID = "MC";

    // Mantenemos solo el canal en las properties
    @Value("${discord.mc.channel-id}")
    private String channelIdProp;

    private Snowflake channelId;

    // Esta variable se llenará dinámicamente usando el cliente
    private Snowflake selfBotId;

    private MessageChannel channel;

    private final GatewayDiscordClient client;
    private final MessageBus bus;

    /**
     * Initializes the DiscordChannelMCAdapter by retrieving the specified Discord channel and setting up listeners for both Discord messages and bus messages.
     */
    @PostConstruct
    public void init() {
        // Inicializamos el Snowflake del canal
        this.channelId = Snowflake.of(this.channelIdProp);

        // Obtenemos el ID del propio bot directamente desde el cliente de Discord4J
        this.selfBotId = this.client.getSelfId();

        this.client.getChannelById(this.channelId)
                .cast(MessageChannel.class)
                .doOnNext(ch -> {

                    this.channel = ch;

                    System.out.println(
                            "Discord channel ready: "
                                    + this.channelId.asString()
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

    /**
     * Sets up a listener for messages in the specified Discord channel.
     * It filters out system messages and messages sent by the bot itself, and publishes valid messages to the message bus.
     */
    private void listenDiscord() {

        this.client.on(MessageCreateEvent.class)
                .filter(event ->
                        event.getMessage()
                                .getChannelId()
                                .equals(this.channelId))
                .filter(event ->
                        !event.getMessage()
                                .getContent()
                                .startsWith("[SYS]"))
                .filter(event ->
                        event.getMessage()
                                .getAuthor()
                                .map(user ->
                                        !user.getId()
                                                .equals(this.selfBotId)) // Filtrará dinámicamente usando el ID obtenido del cliente
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

    /**
     * Sets up a listener for messages on the message bus.
     * It filters out messages originating from this adapter and sends valid messages to the Discord channel.
     */
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

    /**
     * Sends a message to the Discord channel.
     *
     * @param message The BusMessage to be sent to Discord.
     * @return A Mono that completes when the message has been sent.
     */
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