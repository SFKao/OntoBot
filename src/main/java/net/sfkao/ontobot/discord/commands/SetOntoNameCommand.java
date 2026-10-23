package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import lombok.RequiredArgsConstructor;
import net.sfkao.ontobot.discord.AvatarCache;
import net.sfkao.ontobot.onto.OntoUsernameToDiscordUsernameMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SetOntoNameCommand implements DiscordCommandEvent<ChatInputInteractionEvent> {

    private final OntoUsernameToDiscordUsernameMapper ontoUsernameToDiscordUsernameMapper;
    private final AvatarCache avatarCache;

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getCommandName())
                .description("Actualiza el nombre de Ontogether de un usuario en el bot.")
                .dmPermission(false)
                .addOption(
                        ImmutableApplicationCommandOptionData.builder()
                                .name("nombre")
                                .description("El nombre en el Ontogether")
                                .type(3) // 3 = STRING
                                .required(true)
                                .build()
                )
                .addOption(
                        ImmutableApplicationCommandOptionData.builder()
                                .name("usuario")
                                .description("El usuario al que quieres asignarle el nombre (Solo Administradores)")
                                .type(6) // 6 = USER
                                .required(false)
                                .build()
                )
                .build();
    }

    @Override
    public String getCommandName() {
        return "set-onto-name";
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

        final var usuarioOption = event.getOption("usuario");
        final boolean haSeleccionadoOtroUsuario = usuarioOption.isPresent();

        // 1. Validar permisos si intenta cambiar el nombre de otra persona
        if (haSeleccionadoOtroUsuario) {
            final Member ejecutor = event.getInteraction().getMember()
                    .orElseThrow(() -> new IllegalStateException("Comando ejecutado fuera de un servidor"));

            // Verificamos si el ejecutor NO es administrador
            final boolean esAdmin = ejecutor.getBasePermissions()
                    .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                    .blockOptional()
                    .orElse(false);

            if (!esAdmin) {
                return event.reply("No tienes permisos de Administrador para cambiar el nombre de otros usuarios.")
                        .withEphemeral(true); // El mensaje de error solo lo verá él
            }
        }

        // 2. Obtener el usuario objetivo (si se pasó el parámetro, se resuelve; si no, es quien ejecuta)
        final Mono<User> targetUserMono = usuarioOption
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .orElse(Mono.just(event.getInteraction().getUser()));

        // 3. Obtener el nombre del Ontogether
        final String ontoName = event.getOption("nombre")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(() -> new IllegalArgumentException("No se proporcionó un nombre de Ontogether"));

        return targetUserMono.flatMap(targetUser -> {
            final String discordUsername = targetUser.getUsername();

            // Intentar obtener el Display Name dentro del servidor
            final String displayName = event.getInteraction().getGuild()
                    .flatMap(guild -> guild.getMemberById(targetUser.getId()))
                    .map(Member::getDisplayName)
                    .defaultIfEmpty(discordUsername)
                    .block();

            // 4. Limpiar y mapear en el sistema
            this.ontoUsernameToDiscordUsernameMapper.clearMapping(displayName);
            this.ontoUsernameToDiscordUsernameMapper.clearMapping(discordUsername);
            this.ontoUsernameToDiscordUsernameMapper.putMapping(ontoName, displayName);

            // 5. Limpiar la caché del avatar correspondiente
            this.avatarCache.clearForUsername(displayName);
            this.avatarCache.clearForUsername(discordUsername);

            return event.reply("¡El nombre de Ontogether para **" + discordUsername + "** ha sido actualizado correctamente a **" + ontoName + "**!");
        });
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        return Mono.error(error);
    }
}
