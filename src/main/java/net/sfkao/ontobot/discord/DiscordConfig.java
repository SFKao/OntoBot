package net.sfkao.ontobot.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    private static final String TOKEN = "MTQ5MTg1NTE0MzU5NDEwMjg0NQ.Gu29AL.rqF6ioHVTG-GYnNK-AkmJZrp8BezL3Jb3peUmo";
    @Bean
    public GatewayDiscordClient gatewayClient() {

        GatewayDiscordClient client = DiscordClientBuilder.create(TOKEN)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES))
                .login()
                .block();



        return client;
    }
}