package net.sfkao.ontobot.onto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OntoUsernameToDiscordUsernameMapper {

    @Value("${ontobot.onto.discord.username.mapping.filepath}")
    private String mappingFilePath;

    private final Map<String, String> ontogetherToDiscordUsernameMap = new ConcurrentHashMap<>();

    public String mapOntoUsernameToDiscordUsernameOrObtainDefault(final String ontoUsername) {
        return this.ontogetherToDiscordUsernameMap.getOrDefault(ontoUsername, ontoUsername);
    }

    public void putMapping(final String ontoUsername, final String discordUsername) {
        this.ontogetherToDiscordUsernameMap.put(ontoUsername, discordUsername);
        this.persistMappingsToFile();
    }

    public void clearMapping(final String ontoUsername) {
        this.ontogetherToDiscordUsernameMap.remove(ontoUsername);
        this.persistMappingsToFile();
    }

    @PostConstruct
    public void loadMappingsFromFile() {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final File file = new File(this.mappingFilePath);
            if (file.exists()) {
                final Map<String, String> mappings = objectMapper.readValue(file, Map.class);
                this.ontogetherToDiscordUsernameMap.putAll(mappings);
            } else {
                System.err.println("Mapping file not found: " + this.mappingFilePath);
            }
        } catch (final Exception e) {
            System.err.println("Failed to load mappings from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void persistMappingsToFile() {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(this.mappingFilePath), this.ontogetherToDiscordUsernameMap);
        } catch (final Exception e) {
            System.err.println("Failed to persist mappings to file: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
