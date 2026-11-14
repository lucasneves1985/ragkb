package br.lcn.ragkb.dto;

/**
 * Entrada leve do diretório de usuários (V20): alimenta o picker de
 * compartilhamento de projetos — acessível a qualquer usuário autenticado
 * (o GET /users completo permanece ADMIN-only). Sem e-mail, telefone, roles
 * ou setor: o mínimo para identificar e selecionar um participante.
 */
public record UserDirectoryResponse(
        Long id,
        String username,
        String fullName) {
}