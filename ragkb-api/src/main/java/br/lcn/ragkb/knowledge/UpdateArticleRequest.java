package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UpdateArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotEmpty List<String> allowedSectors
) {}