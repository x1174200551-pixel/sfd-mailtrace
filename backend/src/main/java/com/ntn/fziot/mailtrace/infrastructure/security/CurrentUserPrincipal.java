package com.ntn.fziot.mailtrace.infrastructure.security;

public record CurrentUserPrincipal(
        Long id,
        String account,
        String displayName,
        String email,
        String roleCode
) {
}
