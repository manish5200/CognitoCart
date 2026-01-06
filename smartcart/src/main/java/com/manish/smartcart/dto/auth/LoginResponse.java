package com.manish.smartcart.dto.auth;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class LoginResponse {

    private String token;
    private int status;
    private String role;
}
