package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;

import java.util.Set;

public interface EneCommand {
    Set<String> getKeywords();

    CommandPriority getPriority();

    BusMessage execute(BusMessage busMessage);

}
