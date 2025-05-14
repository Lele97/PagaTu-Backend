package com.pagatu.auth.event;


import com.pagatu.auth.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private EventType eventType;
}
