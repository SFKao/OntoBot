package net.sfkao.ontobot.onto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.sfkao.ontobot.bus.BusMessage;
import net.sfkao.ontobot.bus.MessageBus;
import net.sfkao.ontobot.constants.SourceConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OntoAdapter {

    public static final String SOURCE_ID =
            "ONTO";

    private static final String WS_URL =
            "ws://127.0.0.1:8765/";

    public static final String WEBSOCKET_STARTED_MESSAGE =
            "[WSM] Client connected: 127.0.0.1";

    static boolean iniciado = false;

    private final MessageBus bus;
    private final MessageFormatter formatter;
    private final MessageDeduplicator deduplicator;

    private final ReactorNettyWebSocketClient webSocketClient =
            new ReactorNettyWebSocketClient();

    /*
     * Stream persistente de salida
     */
    private final Sinks.Many<String> outgoing =
            Sinks.many()
                    .multicast()
                    .onBackpressureBuffer();

    @PostConstruct
    public void init() {

        connect();

        listenBus();
    }

    @SneakyThrows
    private void connect() {

        Thread.sleep(3000);

        webSocketClient.execute(
                        URI.create(WS_URL),
                        session -> {

                            System.out.println(
                                    "WebSocket connected"
                            );

                            /*
                             * Entrada
                             */
                            Mono<Void> inbound =
                                    session.receive()
                                            .map(WebSocketMessage::getPayloadAsText)
                                            .doOnNext(this::handleIncomingMessage)
                                            .doOnError(error -> {

                                                System.err.println(
                                                        "WebSocket inbound error"
                                                );

                                                error.printStackTrace();
                                            })
                                            .doFinally(signal ->
                                                    System.out.println(
                                                            "WebSocket disconnected"
                                                    )
                                            )
                                            .then();

                            /*
                             * Salida persistente
                             */
                            Mono<Void> outbound =
                                    session.send(
                                            outgoing.asFlux()
                                                    .map(session::textMessage)
                                    );

                            /*
                             * Ejecuta ambos flujos simultáneamente
                             */
                            return Mono.when(
                                    inbound,
                                    outbound
                            );
                        }
                )
                .retryWhen(
                        Retry.fixedDelay(
                                Long.MAX_VALUE,
                                Duration.ofSeconds(5)
                        )
                )
                .doOnError(error -> {

                    System.err.println(
                            "WebSocket connection error"
                    );

                    error.printStackTrace();
                })
                .subscribe(
                        null,
                        error -> {
                            System.err.println("Reactor error:");
                            error.printStackTrace();
                        }
                );
    }

    private void handleIncomingMessage(
            String content
    ) {

        if (!iniciado
                && content.equals(
                WEBSOCKET_STARTED_MESSAGE
        )) {
            content = "[SYS]Onto Abierto!";
            iniciado = true;
        }

        if (content.startsWith("[WSM]")) {
            return;
        }

        // Si el mensaje es una notificacion
        if(content.startsWith("[SYS]")){
            if(!content.contains("se ha unido al servidor!") && !content.contains( "has left the server.") && !content.contains("Onto Abierto!"))
                return;
        }

        content = formatter.cleanMessage(content);

        /*
         * Echo del websocket
         */
        for(String sourceId : SourceConstants.SOURCES){
            if(content.contains("[" + sourceId + "]")){
                return;
            }
        }

        String author;
        if(content.startsWith("[SYS]")){
            author = "Onto";
        }else{
            author = content.split(":")[0].replace("**", "").trim();
            content = content.substring(content.indexOf(":") + 1).trim();
        }

        bus.publish(new BusMessage(
                SOURCE_ID,
                author,
                content,
                Instant.now()
        ));
    }

    private void listenBus() {

        bus.flux()
                .filter(msg ->
                        !msg.sourceId()
                                .equals(SOURCE_ID))
                .subscribe(this::sendToWebSocket);
    }

    private void sendToWebSocket(
            BusMessage message
    ) {

        /*
         * Guardamos el contenido limpio
         * porque el websocket devuelve
         * el mensaje sin tags
         */

        deduplicator.put(message);

        String formatted =
                formatter.format(message);

        Sinks.EmitResult result =
                outgoing.tryEmitNext(formatted);

        if (result.isFailure()) {

            System.err.println(
                    "Failed to send websocket message: "
                            + result
            );
        }
    }
}