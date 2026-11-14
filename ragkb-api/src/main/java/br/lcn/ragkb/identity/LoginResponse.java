package br.lcn.ragkb.identity;



public record LoginResponse(
        String token,
        long expiresIn
) {}