package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EneCommandDiscord implements EneCommand {


    @Override
    public Set<String> getKeywords() {
        return Set.of("discord");
    }

    @Override
    public CommandPriority getPriority() {
        return CommandPriority.NORMAL;
    }

    @Override
    public BusMessage execute(final BusMessage busMessage) {
        final String responseContent = "Enlace al discord marchando! http://s.sfkao.net/dch";
        return new BusMessage(Ene.SOURCE_ID, "Ene", responseContent, busMessage.timestamp());
    }
}
