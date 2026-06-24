package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sfkao.ontobot.bot.EneCommand8Ball;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Log4j2
@RequiredArgsConstructor
public class InvokeKnowledgeCommand implements DiscordCommandEvent<ChatInputInteractionEvent> {

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getCommandName())
                .description("Pidele su gran sabiduria a Ene")
                .dmPermission(true)
                .contexts(0, 1, 2)
                .build();
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(final ChatInputInteractionEvent event) {
        return event.reply(
                EneCommand8Ball.getRandomResponse()
        ).then();
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        InvokeKnowledgeCommand.log.error(error);
        return Mono.empty();
    }

    @Override
    public String getCommandName() {
        return "8ball";
    }

}
