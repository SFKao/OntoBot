package net.sfkao.ontobot.onto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import net.sfkao.ontobot.bus.BusMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@AllArgsConstructor
public class MessageDeduplicator {

    private MessageFormatter messageFormatter;

    private final Cache<String, Instant> recentMessages =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(10))
                    .build();

    public boolean isDuplicate(BusMessage content) {
        return content.sourceId().equals(OntoAdapter.SOURCE_ID);
    }

    public void put(BusMessage content) {

    }

}