package net.sfkao.ontobot.widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador WebSocket servidor con autenticación por contraseña.
 *
 * Flujo de handshake:
 *  1. Cliente conecta.
 *  2. Cliente envía: { "type": "auth", "name": "pepito", "password": "secret" }
 *  3a. Contraseña correcta → auth_ok + aviso al bus + acepta mensajes.
 *  3b. Contraseña incorrecta → auth_fail + cierre de sesión.
 *  4. Mensajes normales: { "type": "message", "name": "...", "message": "..." }
 */
@Component
@RequiredArgsConstructor
public class ChatAdapter {

    public static final String SOURCE_ID = "CHAT";

    private final MessageBus   bus;
    private final ObjectMapper objectMapper;

    /**
     * Contraseña configurada en application.properties:
     *   chat.password=tu_contraseña
     */
    @Value("${chat.password}")
    private String chatPassword;

    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        listenBus();
    }

    // ─── Session lifecycle ────────────────────────────────────────────────────

    public ChatSession onConnect(WebSocketSession session) {
        ChatSession chatSession = new ChatSession(session);
        sessions.put(session.getId(), chatSession);
        System.out.printf("[CHAT] Client connected (pending auth): %s%n", session.getId());
        return chatSession;
    }

    public void onDisconnect(WebSocketSession session) {
        ChatSession removed = sessions.remove(session.getId());
        if (removed == null) return;

        removed.complete();

        // Solo avisar al bus si el usuario había autenticado
        if (removed.isAuthenticated() && removed.getName() != null) {
            System.out.printf("[CHAT] User disconnected: %s%n", removed.getName());
            bus.publish(new BusMessage(
                    SOURCE_ID,
                    "Chat",
                    removed.getName() + " ha abandonado el chat.",
                    Instant.now()
            ));
        } else {
            System.out.printf("[CHAT] Unauthenticated client disconnected: %s%n", session.getId());
        }
    }

    public void onMessage(WebSocketSession session, String raw) {
        ChatSession chatSession = sessions.get(session.getId());
        if (chatSession == null) return;

        ChatWireMessage wire = parseWire(raw);
        if (wire == null) {
            System.err.printf("[CHAT] Malformed message from %s: %s%n", session.getId(), raw);
            return;
        }

        if (!chatSession.isAuthenticated()) {
            handleAuth(chatSession, wire);
        } else {
            handleMessage(chatSession, wire);
        }
    }

    public ChatSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    // ─── Auth handshake ───────────────────────────────────────────────────────

    private void handleAuth(ChatSession chatSession, ChatWireMessage wire) {
        if (!ChatWireUtils.isAuth(wire)) {
            // Rechazar cualquier mensaje que no sea el handshake
            chatSession.send(toJson(ChatWireMessage.authFail("Autentícate primero.")));
            closeSession(chatSession);
            return;
        }

        String name     = wire.name()     != null ? wire.name().trim()     : "";
        String password = wire.password() != null ? wire.password().trim() : "";

        if (name.isEmpty()) {
            chatSession.send(toJson(ChatWireMessage.authFail("El nombre no puede estar vacío.")));
            closeSession(chatSession);
            return;
        }

        if (!chatPassword.equals(password)) {
            System.out.printf("[CHAT] Auth failed for '%s' from %s%n",
                    name, chatSession.getSession().getId());
            chatSession.send(toJson(ChatWireMessage.authFail("Contraseña incorrecta.")));
            closeSession(chatSession);
            return;
        }

        // ✓ Autenticado
        chatSession.markAuthenticated(name);
        chatSession.send(toJson(ChatWireMessage.authOk()));

        System.out.printf("[CHAT] User authenticated: %s (%s)%n",
                name, chatSession.getSession().getId());

        // Avisar al bus de que el usuario se ha unido
        bus.publish(new BusMessage(
                SOURCE_ID,
                "Chat",
                name + " se ha unido al chat.",
                Instant.now()
        ));
    }

    // ─── Chat messages ────────────────────────────────────────────────────────

    private void handleMessage(ChatSession chatSession, ChatWireMessage wire) {
        if (!ChatWireUtils.isMessage(wire)|| wire.message() == null || wire.message().isBlank()) return;

        bus.publish(new BusMessage(
                SOURCE_ID,
                chatSession.getName(),   // Usamos el nombre autenticado, no el del payload
                wire.message().trim(),
                Instant.now()
        ));
    }

    // ─── Bus listener ─────────────────────────────────────────────────────────

    private void listenBus() {
        // Mensajes de otras fuentes (Onto, etc.) → todos los clientes chat
        bus.flux()
                .filter(msg -> !msg.sourceId().equals(SOURCE_ID))
                .subscribe(this::broadcast);

        // Mensajes de clientes chat → el resto de clientes chat (sin eco al autor)
        bus.flux()
                .filter(msg -> msg.sourceId().equals(SOURCE_ID))
                .subscribe(this::broadcastExcludingAuthor);
    }

    private void broadcast(BusMessage msg) {
        String payload = toJson(ChatWireMessage.chatMessage(msg.author(), msg.content()));
        if (payload == null) return;
        sessions.values().stream()
                .filter(ChatSession::isAuthenticated)
                .forEach(s -> s.send(payload));
    }

    private void broadcastExcludingAuthor(BusMessage msg) {
        String payload = toJson(ChatWireMessage.chatMessage(msg.author(), msg.content()));
        if (payload == null) return;
        sessions.values().stream()
                .filter(ChatSession::isAuthenticated)
                .filter(s -> !msg.author().equals(s.getName()))
                .forEach(s -> s.send(payload));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void closeSession(ChatSession chatSession) {
        chatSession.getSession().close().subscribe();
    }

    private ChatWireMessage parseWire(String raw) {
        try {
            return objectMapper.readValue(raw, ChatWireMessage.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toJson(ChatWireMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            System.err.println("[CHAT] Serialization error: " + e.getMessage());
            return null;
        }
    }
}