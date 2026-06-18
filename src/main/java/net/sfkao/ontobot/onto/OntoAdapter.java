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

/**
 * OntoAdapter is a component that connects to a WebSocket server and relays messages between the WebSocket and a message bus.
 * It listens for incoming messages from the WebSocket, processes them, and publishes them to the message bus.
 * It also listens for messages from the message bus and sends them to the WebSocket.
 *
 * @author Kao
 */
@Component
@RequiredArgsConstructor
public class OntoAdapter {

    public static final String SOURCE_ID =
            "ONTO";

    /**
     * The URL of the On-Together WebSocket server to connect to.
     * The websocket comes from a mod.
     */
    private static final String WS_URL =
            "ws://127.0.0.1:8765/";

    /**
     * The message indicating that the WebSocket connection has been established.
     * The websocket sends this message when a client connects to the server.
     */
    public static final String WEBSOCKET_STARTED_MESSAGE =
            "[WSM] Client connected: 127.0.0.1";

    static boolean iniciado = false;

    private final MessageBus bus;

    /**
     * The WebSocket client used to connect to the On-Together WebSocket server.
     */
    private final ReactorNettyWebSocketClient webSocketClient =
            new ReactorNettyWebSocketClient();

    /*
     * Stream persistente de salida
     */
    private final Sinks.Many<String> outgoing =
            Sinks.many()
                    .multicast()
                    .onBackpressureBuffer();

    /**
     * Initializes the OntoAdapter by connecting to the WebSocket server and setting up listeners for both WebSocket messages and bus messages.
     */
    @PostConstruct
    public void init() {

        this.connect();

        this.listenBus();
    }

    /**
     * Connects to the On-Together WebSocket server and sets up the inbound and outbound message streams.
     * The inbound stream listens for incoming messages from the WebSocket, processes them, and publishes them to the message bus.
     * The outbound stream listens for messages from the message bus and sends them to the WebSocket.
     * If the connection is lost, it will retry connecting after a fixed delay.
     */
    @SneakyThrows
    private void connect() {

        Thread.sleep(3000);

        this.webSocketClient.execute(
                        URI.create(OntoAdapter.WS_URL),
                        session -> {

                            System.out.println(
                                    "WebSocket connected"
                            );

                            /*
                             * Entrada
                             */
                            final Mono<Void> inbound =
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
                            final Mono<Void> outbound =
                                    session.send(
                                            this.outgoing.asFlux()
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

    /**
     * Handles an incoming message from the WebSocket.
     * It processes the message content, filters out unwanted messages, and publishes valid messages to the message bus.
     *
     * @param content The content of the incoming WebSocket message.
     */
    private void handleIncomingMessage(
            String content
    ) {

        if (!OntoAdapter.iniciado
                && content.equals(
                OntoAdapter.WEBSOCKET_STARTED_MESSAGE
        )) {
            content = "[SYS]Onto Abierto!";
            OntoAdapter.iniciado = true;
        }

        if (content.startsWith("[WSM]")) {
            return;
        }

        // Si el mensaje es una notificacion
        if (content.startsWith("[SYS]")) {
            if (!content.contains("se ha unido al servidor!") && !content.contains("has left the server.") && !content.contains("Onto Abierto!")) {
                return;
            }
        }

        content = MessageFormatter.cleanMessage(content);

        /*
         * Echo del websocket
         */
        for (final String sourceId : SourceConstants.SOURCES) {
            if (content.contains("[" + sourceId + "]")) {
                return;
            }
        }

        final String author;
        if (content.startsWith("[SYS]")) {
            author = "Onto";
        } else {
            author = content.split(":")[0].replace("**", "").trim();
            content = content.substring(content.indexOf(":") + 1).trim();
        }

        this.bus.publish(new BusMessage(
                OntoAdapter.SOURCE_ID,
                author,
                content,
                Instant.now()
        ));
    }

    /**
     * Listens for messages on the message bus and sends them to the WebSocket.
     * It filters out messages originating from this adapter to avoid echoing messages back to the WebSocket.
     */
    private void listenBus() {

        this.bus.flux()
                .filter(msg ->
                        !msg.sourceId()
                                .equals(OntoAdapter.SOURCE_ID))
                .subscribe(this::sendToWebSocket);
    }

    /**
     * Sends a message to the WebSocket.
     * It formats the message and attempts to emit it to the outgoing sink.
     * If the emission fails, it logs an error message.
     *
     * @param message The BusMessage to be sent to the WebSocket.
     */
    private void sendToWebSocket(
            final BusMessage message
    ) {
        final String formatted =
                MessageFormatter.format(message);

        final Sinks.EmitResult result =
                this.outgoing.tryEmitNext(formatted);

        if (result.isFailure()) {

            System.err.println(
                    "Failed to send websocket message: "
                            + result
            );
        }
    }
}