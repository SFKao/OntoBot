package net.sfkao.ontobot.widget;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Modelo del protocolo WebSocket.
 *
 * Auth handshake (cliente → servidor):
 *   { "type": "auth", "name": "pepito", "password": "secret" }
 *
 * Respuesta auth (servidor → cliente):
 *   { "type": "auth_ok" }
 *   { "type": "auth_fail", "reason": "Contraseña incorrecta" }
 *
 * Mensaje de chat (ambas direcciones):
 *   { "type": "message", "name": "pepito", "message": "hola!" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatWireMessage(
        String type,
        String name,
        String message,
        String password,
        String reason
) {

    // ─── Factories ────────────────────────────────────────────────────────────

    public static ChatWireMessage authOk() {
        return new ChatWireMessage("auth_ok", null, null, null, null);
    }

    public static ChatWireMessage authFail(String reason) {
        return new ChatWireMessage("auth_fail", null, null, null, reason);
    }

    public static ChatWireMessage chatMessage(String name, String message) {
        return new ChatWireMessage("message", name, message, null, null);
    }

}