package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
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
                .description("Actualiza tu nombre de Ontogether en el bot para que te identifique correctamente.")
                .dmPermission(false)
                .addOption(
                        ImmutableApplicationCommandOptionData.builder()
                                .name("nombre")
                                .description("Tu nombre en el Ontogether")
                                .type(3) // 3 = STRING
                                .required(true)
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
        this.ontoUsernameToDiscordUsernameMapper.clearMapping(event.getInteraction().getUser().getUsername());
        this.ontoUsernameToDiscordUsernameMapper.putMapping(
                event.getOption("nombre")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElseThrow(() -> new IllegalArgumentException("No se proporcionó un nombre de Ontogether")),
                event.getInteraction().getUser().getUsername()
        );

        this.avatarCache.clearForUsername(event.getInteraction().getUser().getUsername());

        return event.reply("¡Tu nombre de Ontogether ha sido actualizado correctamente!").then();
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        return null;
    }

}
