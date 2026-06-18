package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;

import java.util.Set;

/**
 * Interface representing a command that can be executed by the bot.
 *
 * @author Kao
 */
public interface EneCommand {
    /**
     * Returns the set of keywords associated with this command. These keywords are used to identify and trigger the command when processing user input.
     *
     * @return a set of keywords
     */
    Set<String> getKeywords();

    /**
     * Returns the priority level of this command. The priority determines the order in which commands are executed when multiple commands are triggered by the same input.
     *
     * @return the priority level of the command
     */
    CommandPriority getPriority();

    /**
     * Executes the command based on the provided bus message. The implementation of this method should define the specific actions to be taken when the command is triggered.
     *
     * @param busMessage the bus message containing information relevant to the command execution
     * @return a bus message that may contain responses or further instructions after executing the command
     */
    BusMessage execute(BusMessage busMessage);

}
