package com.pagatu.coffee.util;

import java.util.Set;

public final class ProfileKeys {

    public static final String DEFAULT_AVATAR = "default";
    public static final String DEFAULT_THEME = "classic";

    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "default", "cup", "beans", "steam", "office");

    private static final Set<String> ALLOWED_THEMES = Set.of(
            "classic", "espresso", "latte", "office");

    private ProfileKeys() {
    }

    public static boolean isValidAvatar(String key) {
        return key != null && ALLOWED_AVATARS.contains(key);
    }

    public static boolean isValidTheme(String key) {
        return key != null && ALLOWED_THEMES.contains(key);
    }
}