package net.sfkao.ontobot.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import net.sfkao.ontobot.discord.commands.DiscordCommandEvent;
import net.sfkao.ontobot.discord.commands.DiscordEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DiscordConfig is a Spring configuration class that sets up the Discord client for the application.
 * It reads the Discord bot token from the application properties and creates a GatewayDiscordClient bean.
 *
 * @author Kao
 */
@Configuration
public class DiscordConfig {

    @Value("${DISCORD_TOKEN}")
    private String TOKEN;

    @Value("${guildId}")
    private long guildId;

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayClient(final List<DiscordEvent<T>> events, final List<DiscordCommandEvent<T>> commands) {

        final GatewayDiscordClient client = DiscordClientBuilder.create(this.TOKEN)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS))
                .login()
                .block();

        // Get our application's ID
        final long applicationId = client.getRestClient().getApplicationId().block();

        client.getRestClient()
                .getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(applicationId, commands.stream().map(DiscordCommandEvent::getCommandRequest).collect(Collectors.toList()))
                .subscribe();

        client.getRestClient()
                .getApplicationService()
                .bulkOverwriteGuildApplicationCommand(applicationId, this.guildId, commands.stream().map(DiscordCommandEvent::getCommandRequest).collect(Collectors.toList()))
                .subscribe();


        for (final DiscordEvent<T> event : events) {
            client.on(event.getEventType())
                    .flatMap(event::execute)
                    .onErrorResume(event::error)
                    .subscribe();
        }

        return client;
    }
}