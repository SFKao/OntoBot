package net.sfkao.ontobot.constants;

import net.sfkao.ontobot.discord.DiscordChannelMCAdapter;
import net.sfkao.ontobot.discord.DiscordChannelOntoAdapter;
import net.sfkao.ontobot.onto.OntoAdapter;
import net.sfkao.ontobot.widget.ChatAdapter;

import java.util.List;

public class SourceConstants {



    public static final String DISCORD_CHANNEL_ONTO = DiscordChannelOntoAdapter.SOURCE_ID;
    public static final String DISCORD_CHANNEL_MC = DiscordChannelMCAdapter.SOURCE_ID;
    public static final String ONTO = OntoAdapter.SOURCE_ID;

    public static final List<String> SOURCES = List.of(
            DISCORD_CHANNEL_ONTO,
            DISCORD_CHANNEL_MC,
            ONTO,
            ChatAdapter.SOURCE_ID
    );

}
