package com.pagatu.coffee.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProfileKeysTest {

    @ParameterizedTest
    @ValueSource(strings = {"default", "cup", "beans", "steam", "office"})
    void isValidAvatar_acceptsAllowedKeys(String key) {
        assertTrue(ProfileKeys.isValidAvatar(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "espresso", "latte", "office"})
    void isValidTheme_acceptsAllowedKeys(String key) {
        assertTrue(ProfileKeys.isValidTheme(key));
    }

    @Test
    void isValidAvatar_rejectsUnknownKey() {
        assertFalse(ProfileKeys.isValidAvatar("custom"));
        assertFalse(ProfileKeys.isValidAvatar(null));
    }

    @Test
    void isValidTheme_rejectsUnknownKey() {
        assertFalse(ProfileKeys.isValidTheme("dark"));
        assertFalse(ProfileKeys.isValidTheme(null));
    }
}