package net.sfkao.ontobot.bot;

public enum CommandPriority {
    VERY_LOW,
    LOW,
    NORMAL,
    HIGH,
    VERY_HIGH;

    public final int value;

    CommandPriority() {
        this.value = this.ordinal();
    }

}
