package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThemeColorsDto {

    private String primary;
    private String secondary;
    private String accent;
    private String background;
    private String surface;
    private String text;
}