package net.sfkao.ontobot.discord.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ImportServerCommand implements DiscordCommandEvent<ChatInputInteractionEvent> {

    @Value("${guildId}")
    private long guildId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getCommandName())
                .description("Imports a JSON file and renames channel categories, channels and roles accordingly")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("file")
                        .description("The JSON file exported by /export-server with updated names")
                        .type(ApplicationCommandOption.Type.ATTACHMENT.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public String getCommandName() {
        return "import-server";
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(final ChatInputInteractionEvent event) {
        if (!event.getCommandName().equals(this.getCommandName())) {
            return Mono.empty();
        }

        return event.getInteraction().getMember()
                .map(member -> member.getBasePermissions())
                .orElse(Mono.empty())
                .flatMap(perms -> {
                    if (!perms.contains(Permission.ADMINISTRATOR)) {
                        return event.reply("❌ You don't have permission to use this command.")
                                .withEphemeral(true);
                    }

                    return event.deferReply().withEphemeral(true)
                            .then(Mono.fromCallable(() -> {
                                final var attachment = event.getOption("file")
                                        .flatMap(opt -> opt.getValue())
                                        .map(v -> v.asAttachment())
                                        .orElseThrow(() -> new IllegalArgumentException("No file provided"));

                                final String url = attachment.getUrl();
                                final HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
                                final HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                                return this.objectMapper.readTree(response.body());
                            }))
                            .flatMap(json -> event.getClient().getGuildById(Snowflake.of(this.guildId))
                                    .flatMap(guild -> {

                                        final Flux<Void> renameRoles = Flux.fromIterable(() -> json.path("roles").elements())
                                                .flatMap(node -> {
                                                    final String id = node.path("id").asText();
                                                    final String newName = node.path("name").asText();
                                                    return guild.getRoleById(Snowflake.of(id))
                                                            .flatMap(role -> role.edit(spec -> spec.setName(newName)).then())
                                                            .onErrorResume(e -> {
                                                                System.err.println("[ImportServerCommand] Could not rename role " + id + ": " + e.getMessage());
                                                                return Mono.empty();
                                                            });
                                                });

                                        final Flux<Void> renameCategories = Flux.fromIterable(() -> json.path("categories").elements())
                                                .flatMap(node -> {
                                                    final String id = node.path("id").asText();
                                                    final String newName = node.path("name").asText();
                                                    return guild.getChannelById(Snowflake.of(id))
                                                            .ofType(Category.class)
                                                            .flatMap(cat -> cat.edit(spec -> spec.setName(newName)).then())
                                                            .onErrorResume(e -> {
                                                                System.err.println("[ImportServerCommand] Could not rename category " + id + ": " + e.getMessage());
                                                                return Mono.empty();
                                                            });
                                                });

                                        final Flux<Void> renameChannels = Flux.fromIterable(() -> json.path("channels").elements())
                                                .flatMap(node -> {
                                                    final String id = node.path("id").asText();
                                                    final String newName = node.path("name").asText();
                                                    return guild.getChannelById(Snowflake.of(id))
                                                            .flatMap(channel -> {
                                                                if (channel instanceof TextChannel) {
                                                                    return ((TextChannel) channel).edit(spec -> spec.setName(newName)).then();
                                                                } else if (channel instanceof VoiceChannel) {
                                                                    return ((VoiceChannel) channel).edit(spec -> spec.setName(newName)).then();
                                                                }
                                                                return Mono.empty();
                                                            })
                                                            .onErrorResume(e -> {
                                                                System.err.println("[ImportServerCommand] Could not rename channel " + id + ": " + e.getMessage());
                                                                return Mono.empty();
                                                            });
                                                });

                                        return renameRoles
                                                .thenMany(renameCategories)
                                                .thenMany(renameChannels)
                                                .then();
                                    }))
                            .then(event.editReply()
                                    .withContentOrNull("✅ Server structure updated successfully from the provided JSON file.")
                                    .then())
                            .onErrorResume(e -> event.editReply()
                                    .withContentOrNull("❌ Error processing the file: " + e.getMessage())
                                    .then());
                });
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        return Mono.fromRunnable(() ->
                System.err.println("[ImportServerCommand] Error: " + error.getMessage()));
    }
}