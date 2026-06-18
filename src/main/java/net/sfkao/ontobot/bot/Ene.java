package net.sfkao.ontobot.bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class Ene {

    public static String SOURCE_ID = "ENE";

    private final MessageBus bus;

    private final List<EneCommand> commands;

    private final Pattern commandPattern = Pattern.compile("(?i)^ene[\\s,]");

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

    private Mono<Void> checkForCommand(final BusMessage busMessage) {
        if (busMessage.sourceId().equals(Ene.SOURCE_ID)) {
            return Mono.empty();
        }
        if (this.commandPattern.matcher(busMessage.content()).find()) {
            this.processCommand(busMessage);
        }
        return Mono.empty();
    }

    private void processCommand(final BusMessage busMessage) {
        //Check that only one command triggers
        EneCommand commandToExecute = null;
        for (final EneCommand command : this.commands) {
            for (final String keyword : command.getKeywords()) {
                if (busMessage.content().toLowerCase().contains(keyword.toLowerCase())) {
                    if (commandToExecute == null) {
                        commandToExecute = command;
                    } else if (commandToExecute != command) {
                        // More than one command triggered, ignore
                        return;
                    }
                }
            }
        }
        if (commandToExecute != null) {
            final BusMessage response = commandToExecute.execute(busMessage);
            this.bus.publish(response);
        }
    }
}
