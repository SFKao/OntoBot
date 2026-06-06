package net.sfkao.ontobot.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    @Value("${discord.token}")
    private String TOKEN;

    @Bean
    public GatewayDiscordClient gatewayClient() {

        final GatewayDiscordClient client = DiscordClientBuilder.create(this.TOKEN)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS))
                .login()
                .block();


        return client;
    }
}