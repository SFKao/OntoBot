package net.sfkao.ontobot.bot;

/**
 * Enum representing the priority levels of commands.
 *
 * @author Kao
 */
public enum CommandPriority {
    VERY_LOW,
    LOW,
    NORMAL,
    HIGH,
    VERY_HIGH;

    /**
     * The integer value of the priority level.
     */
    public final int priorityInt;

    CommandPriority() {
        this.priorityInt = this.ordinal();
    }

}
