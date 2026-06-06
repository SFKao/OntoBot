package net.sfkao.ontobot.discord;


import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AvatarCache {

    /**
     * username → avatar URL, or NO_AVATAR if the user was not found in the guild.
     */
    private final Map<String, String> avatarCache = new ConcurrentHashMap<>();

    public String get(final String username) {
        return this.avatarCache.get(username);
    }

    public void put(final String username, final String url) {
        this.avatarCache.put(username, url);
    }
}
