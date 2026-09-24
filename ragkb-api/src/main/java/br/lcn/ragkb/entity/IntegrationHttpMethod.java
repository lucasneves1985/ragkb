package br.lcn.ragkb.entity;

/**
 * Método HTTP da integração. GET não envia body — parâmetros vão na URL (path
 * ou query) via placeholders {{param}}. POST mantém o request_template.
 */
public enum IntegrationHttpMethod {
    GET,
    POST
}
