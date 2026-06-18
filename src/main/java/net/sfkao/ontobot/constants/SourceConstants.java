package net.sfkao.ontobot.constants;

import net.sfkao.ontobot.bot.Ene;
import net.sfkao.ontobot.discord.DiscordChannelMCAdapter;
import net.sfkao.ontobot.discord.DiscordChannelOntoAdapter;
import net.sfkao.ontobot.onto.OntoAdapter;
import net.sfkao.ontobot.widget.ChatAdapter;

import java.util.List;

/**
 * This class contains constants representing the source identifiers for various adapters used in the OntoBot application.
 * It provides a centralized location for managing source IDs, making it easier to reference them throughout the codebase.
 *
 * @author Kao
 */
public class SourceConstants {

    public static final String DISCORD_CHANNEL_ONTO = DiscordChannelOntoAdapter.SOURCE_ID;
    public static final String DISCORD_CHANNEL_MC = DiscordChannelMCAdapter.SOURCE_ID;
    public static final String ONTO = OntoAdapter.SOURCE_ID;

    public static final String CHAT = ChatAdapter.SOURCE_ID;

    public static final String ENE = Ene.SOURCE_ID;


    public static final List<String> SOURCES = List.of(
            DISCORD_CHANNEL_ONTO,
            DISCORD_CHANNEL_MC,
            ONTO,
            CHAT,
            ENE
    );

}
