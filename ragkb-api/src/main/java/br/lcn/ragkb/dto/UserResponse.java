package br.lcn.ragkb.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        List<String> roles,
        Long sectorId,
        String sectorName,
        boolean enabled
) {}