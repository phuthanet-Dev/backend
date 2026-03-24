package com.mysticcard.backend.dto.auth;

import com.mysticcard.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean isNewUser;
    private boolean needsProfile;
    private User user;
}
