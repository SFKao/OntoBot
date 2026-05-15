package net.sfkao.ontobot.discord;


import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AvatarCache {

    /** username → avatar URL, or NO_AVATAR if the user was not found in the guild. */
    private final Map<String, String> avatarCache = new ConcurrentHashMap<>();


    public String get(String username) {
        return avatarCache.get(username);
    }

    public void put(String username, String url) {
        avatarCache.put(username, url);
    }
}
