package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of the EneCommand interface that simulates a dice roll (d20).
 * When executed, it responds with a random number between 1 and 20, simulating a dice roll.
 * Each user gets a consistent roll for the same day, based on their username and the current date.
 *
 * @author Kao
 */
@Component
public class EneCommandTirada implements EneCommand {


    @Override
    public Set<String> getKeywords() {
        return Set.of("tirada", "dado", "dados", "d20", "roll", "roll20", "tiradas");
    }

    @Override
    public CommandPriority getPriority() {
        return CommandPriority.LOW;
    }

    @Override
    public BusMessage execute(final BusMessage busMessage) {
        final String today = LocalDate.now().toString();
        final Random random = new Random(busMessage.author().hashCode() + today.hashCode());
        final String responseContent = "Tu tirada del dado de hoy es...  ¡" + (random.nextInt(20) + 1 + "!");
        return new BusMessage(Ene.SOURCE_ID, "Ene", responseContent, busMessage.timestamp());
    }
}
