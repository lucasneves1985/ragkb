package br.lcn.ragkb.businessrule;



import java.util.List;

import jakarta.validation.constraints.*;

public record CreateBusinessRuleRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 120) String requester,
        @Size(max = 500) String reason,
        @NotEmpty List<String> sectorNames,
        List<String> articleIds) {
}