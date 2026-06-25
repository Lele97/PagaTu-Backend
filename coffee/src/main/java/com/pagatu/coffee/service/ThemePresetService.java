package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.ThemeColorsDto;
import com.pagatu.coffee.dto.ThemePresetDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThemePresetService {

    private static final List<ThemePresetDto> PRESETS = List.of(
            preset("classic", "Classico",
                    "#6F4E37", "#F5E6D3", "#C8A882", "#FFF8F0", "#FFFFFF", "#2C1810"),
            preset("espresso", "Espresso",
                    "#3B2314", "#1A120B", "#D4A574", "#1F1510", "#2A1F18", "#F5E6D3"),
            preset("latte", "Latte",
                    "#A67B5B", "#F9F1E7", "#E8C9A0", "#FFFCF7", "#FFFFFF", "#4A3728"),
            preset("office", "Ufficio",
                    "#4A5568", "#EDF2F7", "#718096", "#F7FAFC", "#FFFFFF", "#1A202C"));

    public List<ThemePresetDto> getPresets() {
        return PRESETS;
    }

    private static ThemePresetDto preset(String key, String label,
            String primary, String secondary, String accent,
            String background, String surface, String text) {
        return new ThemePresetDto(key, label,
                new ThemeColorsDto(primary, secondary, accent, background, surface, text));
    }
}