package br.lcn.ragkb.dto;

public record LoginResponse(
        String token,
        long expiresIn
) {}