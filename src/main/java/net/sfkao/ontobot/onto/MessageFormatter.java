package net.sfkao.ontobot.onto;

import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.discord.DiscordChannelMCAdapter;
import net.sfkao.ontobot.discord.DiscordChannelOntoAdapter;
import net.sfkao.ontobot.widget.ChatAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessageFormatter {

    public static final Map<String, String> COLORS =
            Map.of(
                    DiscordChannelMCAdapter.SOURCE_ID, "#ff0000",
                    DiscordChannelOntoAdapter.SOURCE_ID, "#5865F2",
                    OntoAdapter.SOURCE_ID, "#00aaff",
                    ChatAdapter.SOURCE_ID, "#00ff00"
            );

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

    public static String cleanMessage(String content) {

        content = content.replace("</color>", "");
        final AtomicReference<String> s = new AtomicReference<>(content);
        MessageFormatter.COLORS.values().forEach(c ->
                s.set(s.get().replace("<color=" + c + ">", ""))
        );
        return s.get();
    }
}