package net.sfkao.ontobot.widget;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Sesión WebSocket activa con su propio sink de salida.
 *
 * Se usa multicast (directBestEffort) en lugar de unicast para evitar que
 * el sink quede en estado NO_SUBSCRIBER si el outbound flux se recrea.
 */
public class ChatSession {

    @Getter
    private final WebSocketSession session;

    @Getter @Setter
    private String name;

    @Getter
    private volatile boolean authenticated = false;

    /*
     * directBestEffort: emite al suscriptor activo; si no hay ninguno en ese
     * momento simplemente descarta (mejor que bloquear o fallar).
     * Para una única conexión WebSocket, siempre habrá exactamente un suscriptor.
     */
    private final Sinks.Many<String> sink = Sinks.many()
            .multicast()
            .directBestEffort();

    public ChatSession(WebSocketSession session) {
        this.session = session;
    }

    public void markAuthenticated(String name) {
        this.name          = name;
        this.authenticated = true;
    }

    public void send(String payload) {
        Sinks.EmitResult result = sink.tryEmitNext(payload);
        if (result.isFailure()) {
            System.err.printf("[CHAT] Emit failed for %s (%s): %s%n",
                    name != null ? name : "?", session.getId(), result);
        }
    }

    public Flux<String> outgoingFlux() {
        return sink.asFlux();
    }

    public void complete() {
        sink.tryEmitComplete();
    }
}