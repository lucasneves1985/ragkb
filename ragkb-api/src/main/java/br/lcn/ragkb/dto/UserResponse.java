package br.lcn.ragkb.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        List<String> roles,
        boolean enabled
) {}