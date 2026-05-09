package net.sfkao.ontobot.onto;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class MessageDeduplicator {

    private final Cache<String, Instant> recentMessages =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(10))
                    .build();

    public boolean isDuplicate(String content) {
        String[] split = content.split("] ", 2);
        if(split.length==1){
            return false;
        }

        content = split[1];

        Instant existing = recentMessages.getIfPresent(content);

        if (existing != null) {
            return true;
        }

        recentMessages.put(content, Instant.now());

        return false;
    }

    public void put(String content) {
        recentMessages.put(content, Instant.now());
    }
}