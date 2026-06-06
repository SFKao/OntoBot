package net.sfkao.ontobot.widget;

public class ChatWireUtils {

    private ChatWireUtils() {}

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public static boolean isAuth(ChatWireMessage message)    { return "auth".equals(message.type()); }

    public static boolean isMessage(ChatWireMessage message) { return message.type() == null || "message".equals(message.type()); }

}
