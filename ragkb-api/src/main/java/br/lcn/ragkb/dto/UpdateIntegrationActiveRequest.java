package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload do PATCH /{id}/active (toggle de ativação). Endpoint dedicado:
 * desativar/ativar NÃO deve exigir reenviar URL, credenciais, templates nem
 * schemas — cada campo @NotNull novo no UpdateIntegrationRequest quebraria o
 * PUT completo do toggle.
 */
public record UpdateIntegrationActiveRequest(
        @NotNull
        Boolean active
        ) {

}
