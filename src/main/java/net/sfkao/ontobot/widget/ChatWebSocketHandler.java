package net.sfkao.ontobot.widget;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatAdapter adapter;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        System.out.printf("[CHAT] New WS session: %s%n", session.getId());

        ChatSession chatSession = adapter.onConnect(session);

        /*
         * Outbound: se suscribe al sink de la sesión ANTES de que inbound empiece,
         * para que el auth_ok (u otros mensajes tempranos) no se pierdan.
         */
        Mono<Void> outbound = session
                .send(chatSession.outgoingFlux().map(session::textMessage))
                .doOnError(err ->
                        System.err.printf("[CHAT] Outbound error for %s: %s%n",
                                session.getId(), err.getMessage()))
                .onErrorComplete(); // No dejar que un error de escritura mate la sesión entera

        /*
         * Inbound: procesa mensajes y desconecta al terminar.
         */
        Mono<Void> inbound = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .doOnNext(raw -> {
                    System.out.printf("[CHAT] Received from %s: %s%n", session.getId(), raw);
                    adapter.onMessage(session, raw);
                })
                .doOnError(err ->
                        System.err.printf("[CHAT] Inbound error on %s: %s%n",
                                session.getId(), err.getMessage()))
                .doFinally(signal -> {
                    System.out.printf("[CHAT] Session closed (%s): %s%n",
                            signal, session.getId());
                    adapter.onDisconnect(session);
                })
                .then();

        // Outbound primero para que el sink ya tenga suscriptor cuando llegue auth_ok
        return outbound.mergeWith(inbound).then();
    }
}