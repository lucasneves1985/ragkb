package br.lcn.ragkb.entity;

import br.lcn.ragkb.exception.InvalidRoleException;
import java.util.List;

public enum AppRole {
    ROLE_USER,
    ROLE_EDITOR,
    ROLE_ADMIN;

    private static final String PREFIX = "ROLE_";

    public static AppRole parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRoleException(raw);
        }
        String normalized = raw.trim().toUpperCase();
        if (normalized.startsWith(PREFIX)) {
            normalized = normalized.substring(PREFIX.length());
        }
        try {
            return AppRole.valueOf(PREFIX + normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException(raw);
        }
    }

    public String simpleName() {
        return name().substring(PREFIX.length());
    }

    public String authority() {
        return name();
    }

    public static List<String> allAuthorities() {
        return List.of(ROLE_USER.authority(), ROLE_EDITOR.authority(), ROLE_ADMIN.authority());
    }
}