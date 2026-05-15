package net.sfkao.ontobot.onto;

import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.discord.DiscordChannelMCAdapter;
import net.sfkao.ontobot.discord.DiscordChannelOntoAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessageFormatter {

    public static final Map<String, String> COLORS =
            Map.of(
                    DiscordChannelMCAdapter.SOURCE_ID, "#ff0000",
                    DiscordChannelOntoAdapter.SOURCE_ID, "#5865F2",
                    OntoAdapter.SOURCE_ID, "#00aaff"
            );

    public String format(
            BusMessage message
    ) {

        String color =
                COLORS.getOrDefault(
                        message.sourceId(),
                        "#ffffff"
                );

        return "<color=" + color + ">["
                + message.sourceId()
                + "] </color>"
                +message.author()+": "
                + message.content();
    }

    public String cleanMessage(String content) {

        content = content.replace("</color>", "");
        AtomicReference<String> s = new AtomicReference<>(content);
        COLORS.values().forEach(c->
                        s.set(s.get().replace("<color=" + c + ">", ""))
                );
        return s.get();
    }
}