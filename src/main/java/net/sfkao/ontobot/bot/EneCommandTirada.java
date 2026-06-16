package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;

@Component
public class EneCommandTirada implements EneCommand {

    Random random = new Random();

    @Override
    public Set<String> getKeywords() {
        return Set.of("tirada", "dado");
    }

    @Override
    public BusMessage execute(final BusMessage busMessage) {
        final String responseContent = "Tu tirada del dado de hoy es...  ¡" + (this.random.nextInt(20) + 1 + "!");
        return new BusMessage(Ene.SOURCE_ID, "Ene", responseContent, busMessage.timestamp());
    }
}
