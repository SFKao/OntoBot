package net.sfkao.ontobot.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Image;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
@Component
@RequiredArgsConstructor
public class DiscordChannelOntoAdapter {

    public static final String SOURCE_ID =
            "Discord";

    private static final Snowflake CHANNEL_ID =
            Snowflake.of("1492043447639740546");


    /** Sentinel stored in cache when a username has no matching guild member. */
    private static final String NO_AVATAR = "";

    private static final String webhookUrl = "https://discord.com/api/webhooks/1504739568857911296/y7_GjRm5BIQNEfMdZ_nDLGbwMGpR7P6EHrL7350PzOEjkJoeGDS1ljsUw9ZhdvfWtx0Y";

    private Snowflake ownWebhookId;
    private WebhookClient webhookClient;
    private Guild guild;

    /** username → avatar URL, or NO_AVATAR if the user was not found in the guild. */
    private final AvatarCache avatarCache;

    private final GatewayDiscordClient client;
    private final MessageBus bus;

    @PostConstruct
    public void init() {

        String[] parts = webhookUrl.split("/");
        ownWebhookId = Snowflake.of(parts[parts.length - 2]);

        webhookClient = new WebhookClientBuilder(webhookUrl)
                .setWait(false)
                .build();

        // Fetch the guild once so we can search members for avatar lookup.
        client.getChannelById(CHANNEL_ID)
                .cast(GuildMessageChannel.class)
                .flatMap(GuildMessageChannel::getGuild)
                .subscribe(
                        g -> {
                            this.guild = g;
                            System.out.println("Webhook adapter ready — guild: " + g.getName());
                            listenDiscord();
                            listenBus();
                        },
                        error -> {
                            System.err.println("DiscordChannelMCAdapter init error:");
                            error.printStackTrace();
                        }
                );
    }

    @PreDestroy
    public void destroy() {
        if (webhookClient != null) {
            webhookClient.close();
        }
    }

    // -------------------------------------------------------------------------
    // Inbound: Discord → bus
    // -------------------------------------------------------------------------

    private void listenDiscord() {

        client.on(MessageCreateEvent.class)
                .filter(event -> event.getMessage()
                        .getChannelId()
                        .equals(CHANNEL_ID))
                .filter(event -> event.getMessage()
                        .getWebhookId()
                        .map(id -> !id.equals(ownWebhookId))
                        .orElse(true))
                .flatMap(event -> event.getMessage()
                        .getAuthorAsMember()
                        .map(PartialMember::getDisplayName)          // nickname ?? username
                        .defaultIfEmpty(event.getMessage()              // fallback for webhooks /
                                .getUserData().username())              // users outside the guild
                        .map(displayName -> new BusMessage(
                                SOURCE_ID,
                                displayName,
                                event.getMessage().getContent(),
                                Instant.now()
                        )))
                .subscribe(bus::publish);
    }

    // -------------------------------------------------------------------------
    // Outbound: bus → Discord webhook
    // -------------------------------------------------------------------------

    private void listenBus() {

        bus.flux()
                .filter(msg -> !msg.sourceId().equals(SOURCE_ID))
                .flatMap(this::sendToDiscord)
                .subscribe(
                        null,
                        error -> {
                            System.err.println("Discord webhook send failed:");
                            error.printStackTrace();
                        }
                );
    }

    private Mono<Void> sendToDiscord(BusMessage message) {

        if (webhookClient == null) {
            return Mono.empty();
        }

        return resolveAvatarUrl(message.author())
                .flatMap(avatarUrl -> {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                            .setUsername(message.author())
                            .setContent(message.content());

                    if (!avatarUrl.equals(NO_AVATAR)) {
                        builder.setAvatarUrl(avatarUrl);
                    }

                    return Mono.fromFuture(() -> webhookClient.send(builder.build()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }

    // -------------------------------------------------------------------------
    // Avatar resolution (cached)
    // -------------------------------------------------------------------------

    /**
     * Returns a Mono that always emits exactly one value:
     * either the avatar URL of the Discord member whose username matches,
     * or {@link #NO_AVATAR} if no match is found.
     * Results are cached so the guild member list is only queried once per username.
     */
    private Mono<String> resolveAvatarUrl(String username) {

        // Return immediately if already cached (hit or confirmed miss).
        String cached = avatarCache.get(username);
        if (cached != null) {
            return Mono.just(cached);
        }

        if (guild == null) {
            return Mono.just(NO_AVATAR);
        }

        return guild.getMembers()
                .filter(member -> member.getDisplayName().equalsIgnoreCase(username))
                .next()
                .map(member -> member.getAvatarUrl(Image.Format.PNG)
                        .orElseGet(member::getDefaultAvatarUrl))
                // Cache the found URL.
                .doOnNext(url -> avatarCache.put(username, url))
                // If no guild member matched, cache the sentinel and return it.
                .switchIfEmpty(Mono.fromCallable(() -> {
                    avatarCache.put(username, NO_AVATAR);
                    return NO_AVATAR;
                }));
    }
}
