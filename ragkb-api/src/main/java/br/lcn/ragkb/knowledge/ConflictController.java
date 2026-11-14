package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents/conflicts")
@RequiredArgsConstructor
public class ConflictController {

    private final ConflictDetectionService conflictDetectionService;
    private final ConflictCandidateService conflictCandidateService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConflictCandidateResponse> listAll() {
        return conflictCandidateService.listAll();
    }

    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConflictCandidateResponse> scan() {
        conflictDetectionService.scan();
        return conflictCandidateService.listAll();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConflictCandidateResponse updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody ConflictActionRequest request,
                                                  Authentication auth) {
        return conflictCandidateService.updateStatus(id, request, auth.getName());
    }
}