package com.sfonda.mailtrace.infrastructure.security;

public record CurrentUserPrincipal(
        Long id,
        String account,
        String displayName,
        String email,
        String roleCode
) {
}
