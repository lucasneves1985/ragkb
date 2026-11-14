package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record UpdateDocumentAccessRequest(@NotEmpty List<String> allowedSectors) {}
