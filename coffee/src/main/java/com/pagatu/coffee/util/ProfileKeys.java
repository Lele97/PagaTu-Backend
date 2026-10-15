package com.pagatu.coffee.util;

import java.util.Set;

public final class ProfileKeys {

    public static final String DEFAULT_AVATAR = "default";

    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "default", "cup", "beans", "steam", "office");

    private ProfileKeys() {
    }

    public static boolean isValidAvatar(String key) {
        return key != null && ALLOWED_AVATARS.contains(key);
    }
}