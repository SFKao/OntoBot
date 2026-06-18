package net.sfkao.ontobot.onto;

import net.sfkao.ontobot.bot.Ene;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.discord.DiscordChannelMCAdapter;
import net.sfkao.ontobot.discord.DiscordChannelOntoAdapter;
import net.sfkao.ontobot.widget.ChatAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MessageFormatter is a utility class that provides methods for formatting and cleaning messages.
 * It formats messages with color codes based on their source and can clean messages by removing color tags.
 *
 * @author Kao
 */
@Component
public class MessageFormatter {

    public static final Map<String, String> COLORS =
            Map.of(
                    DiscordChannelMCAdapter.SOURCE_ID, "#ff0000",
                    DiscordChannelOntoAdapter.SOURCE_ID, "#5865F2",
                    OntoAdapter.SOURCE_ID, "#00aaff",
                    ChatAdapter.SOURCE_ID, "#00ff00",
                    Ene.SOURCE_ID, "#00FFFF"
            );

    /**
     * Formats a BusMessage into a string with color codes based on its source.
     *
     * @param message The BusMessage to format.
     * @return A formatted string representation of the message.
     */
    public static String format(
            final BusMessage message
    ) {

        final String color =
                MessageFormatter.COLORS.getOrDefault(
                        message.sourceId(),
                        "#ffffff"
                );

        return "<color=" + color + ">["
                + message.sourceId()
                + "] </color><color=#3D3FA1>"
                + message.author() + "</color>: "
                + message.content();
    }

    /**
     * Cleans a message by removing color tags.
     *
     * @param content The message content to clean.
     * @return The cleaned message content without color tags.
     */
    public static String cleanMessage(String content) {

        content = content.replace("</color>", "");
        final AtomicReference<String> s = new AtomicReference<>(content);
        MessageFormatter.COLORS.values().forEach(c ->
                s.set(s.get().replace("<color=" + c + ">", ""))
        );
        return s.get();
    }
}