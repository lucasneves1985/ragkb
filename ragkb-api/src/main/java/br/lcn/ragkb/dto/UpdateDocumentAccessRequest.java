package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateDocumentAccessRequest(@NotEmpty List<String> allowedSectors) {}
