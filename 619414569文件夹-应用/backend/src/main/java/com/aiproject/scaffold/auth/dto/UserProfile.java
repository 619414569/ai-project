package com.aiproject.scaffold.auth.dto;

import java.util.List;

public record UserProfile(String username, List<String> roles, boolean passwordChangeRequired) {
    public UserProfile {
        roles = List.copyOf(roles);
    }
}
