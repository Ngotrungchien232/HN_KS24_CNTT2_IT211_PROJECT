package com.project_cuoimon.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
}
