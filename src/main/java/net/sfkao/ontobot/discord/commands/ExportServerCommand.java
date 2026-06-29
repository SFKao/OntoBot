package net.sfkao.ontobot.discord.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ExportServerCommand implements DiscordCommandEvent<ChatInputInteractionEvent> {

    @Value("${guildId}")
    private long guildId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getCommandName())
                .description("Exports all channel categories, channels and roles with their IDs and names as a JSON file")
                .build();
    }

    @Override
    public String getCommandName() {
        return "export-server";
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
                            .then(event.getClient().getGuildById(Snowflake.of(this.guildId))
                                    .flatMap(guild -> {
                                        final ObjectNode root = this.objectMapper.createObjectNode();

                                        return guild.getRoles()
                                                .filter(role -> !role.isEveryone())
                                                .collectList()
                                                .flatMap(roles -> {
                                                    final ArrayNode rolesArray = this.objectMapper.createArrayNode();
                                                    roles.forEach(role -> {
                                                        final ObjectNode roleNode = this.objectMapper.createObjectNode();
                                                        roleNode.put("id", role.getId().asString());
                                                        roleNode.put("name", role.getName());
                                                        rolesArray.add(roleNode);
                                                    });
                                                    root.set("roles", rolesArray);

                                                    return guild.getChannels().collectList();
                                                })
                                                .flatMap(channels -> {
                                                    final ArrayNode categoriesArray = this.objectMapper.createArrayNode();
                                                    final ArrayNode channelsArray = this.objectMapper.createArrayNode();

                                                    channels.forEach(channel -> {
                                                        if (channel instanceof Category) {
                                                            final ObjectNode catNode = this.objectMapper.createObjectNode();
                                                            catNode.put("id", channel.getId().asString());
                                                            catNode.put("name", channel.getName());
                                                            categoriesArray.add(catNode);
                                                        } else if (channel instanceof GuildChannel) {
                                                            final ObjectNode chanNode = this.objectMapper.createObjectNode();
                                                            chanNode.put("id", channel.getId().asString());
                                                            chanNode.put("name", channel.getName());
                                                            channelsArray.add(chanNode);
                                                        }
                                                    });

                                                    root.set("categories", categoriesArray);
                                                    root.set("channels", channelsArray);

                                                    try {
                                                        System.out.println("[ExportServerCommand] Exporting server structure: " + root.toPrettyString());
                                                        final byte[] jsonBytes = this.objectMapper.writerWithDefaultPrettyPrinter()
                                                                .writeValueAsBytes(root);

                                                        return event.editReply()
                                                                .withContentOrNull("✅ Server structure exported successfully.")
                                                                .withFiles(discord4j.core.spec.MessageCreateFields.File.of(
                                                                        "server-export.json", new java.io.ByteArrayInputStream(jsonBytes)))
                                                                .then();
                                                    } catch (final Exception e) {
                                                        return Mono.error(e);
                                                    }
                                                });
                                    }));
                });
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        return Mono.fromRunnable(() ->
                System.err.println("[ExportServerCommand] Error: " + error.getMessage()));
    }
}