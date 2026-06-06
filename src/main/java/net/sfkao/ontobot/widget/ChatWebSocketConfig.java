package net.sfkao.ontobot.widget;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * Registra el endpoint WebSocket en /ws.
 * Cambia la ruta o añade más entradas al map si necesitas múltiples endpoints.
 */
@Configuration
@RequiredArgsConstructor
public class ChatWebSocketConfig {

    private final ChatWebSocketHandler handler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        return new SimpleUrlHandlerMapping(
                Map.of("/ws", handler),
                -1  // Orden alto para que no colisione con otros handlers
        );
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
