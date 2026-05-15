package net.sfkao.ontobot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.common.util.Snowflake;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GPT {

    // ================= CONFIG =================
    private static final String TOKEN = "MTQ5MTg1NTE0MzU5NDEwMjg0NQ.Gu29AL.rqF6ioHVTG-GYnNK-AkmJZrp8BezL3Jb3peUmo";
    private static final long CHANNEL_ID = 1492043447639740546L;
    private static final String WS_URL = "ws://127.0.0.1:8765/";

    // Prevent infinite loops
    private static final String DISCORD_TAG = "[DC] ";
    //private static final String WS_TAG = "[ONTO] ";
    //public static final String WS_TAG_WITH_ASTERISKS = "**" + WS_TAG;

    // Reconnect settings
    private static final int MAX_RETRIES = Integer.MAX_VALUE;
    private static final int INITIAL_DELAY_SECONDS = 2;
    private static final int MAX_DELAY_SECONDS = 60;
    public static final String WEBSOCKET_STARTED_MESSAGE = "[WSM] Client connected: 127.0.0.1";
    public static final String STARTED_SERVER_MESSAGE = "Servidor del On-together abierto por Kao.";

    // =========================================

    private static GatewayDiscordClient client;
    private static WebSocket webSocket;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean reconnecting = new AtomicBoolean(false);

    public static void main(String[] args) {

        // ================= DISCORD INIT =================
        client = DiscordClientBuilder.create(TOKEN)
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.MESSAGE_CONTENT, Intent.GUILD_MESSAGES))
                .login()
                .block();

        System.out.println("Discord bot connected");

        // ================= WEBSOCKET INIT =================
        connectWebSocket(0);

        // ================= DISCORD LISTENER =================
        client.on(MessageCreateEvent.class)
                .subscribe(event -> {
                    // ⚠️ IMPORTANT: Ignore bot messages to prevent loops
                    if (event.getMessage().getAuthor().map(User::isBot).orElse(false)) {
                        return;
                    }

                    long channelId = event.getMessage().getChannelId().asLong();

                    if (channelId == CHANNEL_ID) {

                        String content = event.getMessage().getContent();

//                        // ⚠️ IMPORTANT: Prevent loop (ignore messages coming from WS)
//                        if (content.startsWith(WS_TAG_WITH_ASTERISKS)) {
//                            return;
//                        }

                        // Send to WebSocket with tag
                        sendToWebSocket(DISCORD_TAG + event.getMessage().getAuthor().map(user -> user.asMember(event.getGuildId().get()).block().getDisplayName()).orElse("") +": " + content);
                    }
                });

        client.onDisconnect().block();
    }

    // ================= WEBSOCKET CONNECTION =================
    private static void connectWebSocket(int attempt) {

        if (reconnecting.getAndSet(true)) {
            return;
        }

        int delay = Math.min(INITIAL_DELAY_SECONDS * (int)Math.pow(2, attempt), MAX_DELAY_SECONDS);

        System.out.println("Connecting WebSocket (attempt " + attempt + ") in " + delay + "s...");

        scheduler.schedule(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                webSocket = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(WS_URL), new WSListener())
                        .join();

                reconnecting.set(false);
                System.out.println("WebSocket connected");

            } catch (Exception e) {
                System.out.println("Connection failed: " + e.getMessage());
                reconnecting.set(false);
                reconnect(attempt + 1);
            }

        }, delay, TimeUnit.SECONDS);
    }

    private static void reconnect(int attempt) {
        if (attempt > MAX_RETRIES) {
            System.out.println("Max retries reached. Giving up.");
            return;
        }
        connectWebSocket(attempt);
    }

    // ================= WEBSOCKET LISTENER =================
    private static class WSListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("WebSocket opened");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {

            String message = data.toString();

            // ⚠️ IMPORTANT: Prevent loop (ignore messages from Discord)
            if (message.contains(DISCORD_TAG)) {
                return WebSocket.Listener.super.onText(ws, data, last);
            }

            if(message.equals(WEBSOCKET_STARTED_MESSAGE))
            {
                sendToDiscord(STARTED_SERVER_MESSAGE);
                return WebSocket.Listener.super.onText(ws, data, last);
            }

            // Si el mensaje es una notificacion
            if(message.startsWith("[SYS]")){
                if(!message.contains("se ha unido al servidor!") && !message.contains( "has left the server."))
                    return WebSocket.Listener.super.onText(ws, data, last);
            }

            sendToDiscord(message);

            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            return WebSocket.Listener.super.onBinary(ws, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            System.out.println("WebSocket closed: " + reason);

            // ⚠️ IMPORTANT: Auto-reconnect on close
            reconnect(0);

            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            System.out.println("WebSocket error: " + error.getMessage());

            // ⚠️ IMPORTANT: Reconnect on error
            reconnect(0);
        }
    }

    // ================= SEND TO WEBSOCKET =================
    private static void sendToWebSocket(String message) {
        if (webSocket != null) {
            webSocket.sendText(message, true);
        } else {
            System.out.println("WebSocket not connected");
        }
    }

    // ================= SEND TO DISCORD =================
    private static void sendToDiscord(String message) {

        client.getChannelById(Snowflake.of(CHANNEL_ID))
                .ofType(MessageChannel.class)
                .subscribe(channel -> {
                    String[] split = message.split(":");
                    StringBuilder sb = new StringBuilder(message.length()+4);
                    sb.append("**").append(split[0]).append("**");
                    for (int i = 1; i < split.length; i++)
                        sb.append(":").append(split[i]);
                    channel.createMessage(sb.toString()).subscribe(
                            null,
                            error -> {
                                System.err.println("Reactor error:");
                                error.printStackTrace();
                            }
                    );
                });
    }
}