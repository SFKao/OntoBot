package net.sfkao.ontobot.bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Listens to the message bus for messages that start with "ene" and processes them using the appropriate EneCommand.
 *
 * @author Kao
 */
@Component
@RequiredArgsConstructor
public class Ene {

    public static String SOURCE_ID = "ENE";

    private final MessageBus bus;

    private final List<EneCommand> commands;

    private final Pattern commandPattern = Pattern.compile("(?i)^ene[\\s,]");

    /**
     * Initializes the Ene component by subscribing to the message bus and checking for commands in incoming messages.
     */
    @PostConstruct
    public void init() {
        this.bus.flux()
                .subscribe(
                        this::checkForCommand,
                        error -> {
                            System.err.println("Discord webhook send failed:");
                            error.printStackTrace();
                        }
                );
    }

    /**
     * Checks if the given BusMessage contains a command for Ene and processes it if it does.
     *
     * @param busMessage The BusMessage to check for commands.
     * @return A Mono that completes when the command check is done.
     */
    private Mono<Void> checkForCommand(final BusMessage busMessage) {
        if (busMessage.sourceId().equals(Ene.SOURCE_ID)) {
            return Mono.empty();
        }
        if (this.commandPattern.matcher(busMessage.content()).find()) {
            this.processCommand(busMessage);
        }
        return Mono.empty();
    }

    /**
     * Processes the given BusMessage by finding and executing the matching EneCommand.
     *
     * @param busMessage The BusMessage to process.
     */
    private void processCommand(final BusMessage busMessage) {
        final List<EneCommand> matchingCommands = this.commandsThatMatch(busMessage);
        if (matchingCommands.isEmpty()) {
            return;
        }
        final EneCommand commandToExecute = matchingCommands.get(0);
        final BusMessage responseMessage = commandToExecute.execute(busMessage);
        this.bus.publish(responseMessage);
    }

    /**
     * Finds the list of EneCommands that match the given BusMessage based on keywords and sorts them by priority.
     *
     * @param busMessage The BusMessage to check for matching commands.
     * @return A list of matching EneCommands sorted by priority.
     */
    private List<EneCommand> commandsThatMatch(final BusMessage busMessage) {
        return this.commands.stream()
                .filter(command -> command.getKeywords().stream()
                        .anyMatch(keyword -> busMessage.content().toLowerCase().contains(keyword.toLowerCase())))
                .sorted((c1, c2) -> c2.getPriority().priorityInt - c1.getPriority().priorityInt)
                .toList();
    }
}
