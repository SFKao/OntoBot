package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.Event;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;

@Component
public interface DiscordCommandEvent<T extends Event> extends DiscordEvent<T> {

    ApplicationCommandRequest getCommandRequest();

    String getCommandName();
}
