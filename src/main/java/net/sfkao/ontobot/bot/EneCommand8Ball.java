package net.sfkao.ontobot.bot;

import net.sfkao.ontobot.bus.BusMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of the EneCommand interface that simulates a Magic 8-Ball.
 * When executed, it provides a random response from a predefined list of possible answers.
 *
 * @author Kao
 */
@Component
public class EneCommand8Ball implements EneCommand {

    private static final Random RANDOM = new Random();

    private static final List<String> RESPONSES = List.of(
            "Sí, definitivamente.",
            "Sin duda.",
            "Definitivamente sí.",
            "No hay duda.",
            "Sí, pero no te confíes.",
            "Pregunta de nuevo más tarde.",
            "No puedo predecirlo ahora.",
            "Concéntrate y pregunta de nuevo.",
            "No cuentes con ello.",
            "Mi respuesta es no.",
            "Mis fuentes dicen que no.",
            "Muy dudoso.",
            "Deberias darle 20€ a Kao ;3",
            "LET IT RIDEEEEE!!!",
            "Pepe Metroguy te extraña mucho, pero no te preocupes, él está bien.",
            "¿Me ves cara de mono de feria? Si tienes alguna duda vas a chatGPT como todo el mundo.",
            "No, pero si quieres puedo hacer que tu futuro sea más brillante con un poco de magia o drogas.",
            "Opino que sí, pero no te fíes de mi opinión, soy un bot y no tengo sentimientos.",
            "No puedo predecir el futuro, pero puedo decirte que tu futuro es brillante y lleno de oportunidades.",
            "Asegurate de guardar con frecuencia.",
            "No deberias quitarle los quesos a la gente, eso es de persona horrible.",
            "Si tienes algun problema nos vemos en los juzgados del Guilty as Sock.",
            "No puedo decirte eso, pero si quieres te puedo dar un consejo de vida: No seas un idiota.",
            "La tortilla de patata es mejor con cebolla.",
            "La tortilla de patata es mejor sin cebolla.",
            "Deberias unirte al servidor de Minecraft. Kao seria muy feliz :3",
            "NO TE COMAS ESOS CACAHUETES!",
            "No se yo, lo que Kao diga.",
            "Nunca seas un cucumber.",
            "Ahora estas respirando manualmente.",
            "Invade Venta de Pantalones.",
            "No deberias hacer eso, pero si lo haces, no me culpes a mi.",
            "Bebe agua.",
            "Seguro que a Hacienda le encantará leer eso.",
            "Cuando le regales un monster a Dubsty responderé a tu pregunta.",
            "No sé sobre eso, pero no te acerques a Rin sin rodilleras porfa.",
            "Rotar una vaca en tu cabeza es gratis y divertido."
    );

    @Override
    public Set<String> getKeywords() {
        return Set.of("8ball", "8-ball", "magic 8 ball", "sabiduria", "conocimiento",
                "destino", "futuro", "pregunta", "respuesta", "suerte", "crees", "piensas",
                "opinas", "dices", "afirmas", "negas", "niegas", "rechazas", "aceptas", "apruebas",
                "desapruebas", "deberia", "tengo", "?",
                "opinion", "consejo", "ayuda", "guia", "orientacion", "recomendacion", "sugerencia",
                "opiniones", "consejos", "ayudas", "guias", "orientaciones", "recomendaciones", "sugerencias");
    }

    @Override
    public CommandPriority getPriority() {
        return CommandPriority.VERY_LOW;
    }

    @Override
    public BusMessage execute(final BusMessage busMessage) {
        return new BusMessage(Ene.SOURCE_ID, "Ene", EneCommand8Ball.getRandomResponse(), busMessage.timestamp());
    }

    public static String getRandomResponse() {
        return "⓼ " + EneCommand8Ball.RESPONSES.get(EneCommand8Ball.RANDOM.nextInt(EneCommand8Ball.RESPONSES.size()));
    }
}
