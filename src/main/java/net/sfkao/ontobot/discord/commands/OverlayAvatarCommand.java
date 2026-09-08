package net.sfkao.ontobot.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

@Component
@Log4j2
@RequiredArgsConstructor
public class OverlayAvatarCommand implements DiscordCommandEvent<ChatInputInteractionEvent> {

    // Coloca tu PNG predefinido en src/main/resources/overlay.png
    private static final String OVERLAY_RESOURCE_PATH = "overlay.png";
    private static final int AVATAR_SIZE = 256;

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
                .name(this.getCommandName())
                .description("Obtén tu foto con un marco especial para el evento!")
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
        if (!event.getCommandName().equals(this.getCommandName())) {
            return Mono.empty();
        }

        // Deferir la respuesta para tener tiempo de procesar la imagen
        return event.deferReply()
                .then(Mono.fromCallable(() -> OverlayAvatarCommand.buildOverlayImage(event))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(imageBytes -> {
                    final MessageCreateFields.File file = MessageCreateFields.File.of(
                            "resultado.png",
                            new ByteArrayInputStream(imageBytes)
                    );
                    return event.createFollowup(
                            InteractionFollowupCreateSpec.builder()
                                    .addFile(file)
                                    .build()
                    );
                })
                .then();
    }

    private static byte[] buildOverlayImage(final ChatInputInteractionEvent event) throws Exception {
        // 1. Obtener la URL del avatar del usuario
        final String avatarUrl = event.getInteraction()
                .getUser()
                .getAvatarUrl()
                + "?size=" + OverlayAvatarCommand.AVATAR_SIZE;

        // 2. Descargar el avatar
        final byte[] avatarBytes = URI.create(avatarUrl).toURL().openStream().readAllBytes();
        final BufferedImage avatar = ImageIO.read(new ByteArrayInputStream(avatarBytes));

        // 3. Escalar el avatar al tamaño base
        final BufferedImage base = new BufferedImage(OverlayAvatarCommand.AVATAR_SIZE, OverlayAvatarCommand.AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = base.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(avatar, 0, 0, OverlayAvatarCommand.AVATAR_SIZE, OverlayAvatarCommand.AVATAR_SIZE, null);

        // 4. Cargar el overlay desde resources y dibujarlo encima
        final InputStream overlayStream = new ClassPathResource(OverlayAvatarCommand.OVERLAY_RESOURCE_PATH).getInputStream();
        final BufferedImage overlay = ImageIO.read(overlayStream);
        g2d.drawImage(overlay, 0, 0, OverlayAvatarCommand.AVATAR_SIZE, OverlayAvatarCommand.AVATAR_SIZE, null);

        g2d.dispose();

        // 5. Convertir a bytes PNG
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(base, "PNG", out);
        return out.toByteArray();
    }

    @Override
    public Mono<Void> error(final Throwable error) {
        OverlayAvatarCommand.log.error("Error en overlay-avatar: ", error);
        return Mono.empty();
    }

    @Override
    public String getCommandName() {
        return "avatar-overlay";
    }
}