package com.pagatu.coffee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAwardDto {

    private String id;
    private String name;
    private String level;
    private String icon;
    private String code;
    private String groupName;
    private LocalDateTime earnedAt;
}
