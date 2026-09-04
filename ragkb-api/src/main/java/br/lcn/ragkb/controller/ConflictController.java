package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.ConflictActionRequest;
import br.lcn.ragkb.entity.ConflictCandidate;
import br.lcn.ragkb.entity.ConflictStatus;
import br.lcn.ragkb.repository.ConflictCandidateRepository;
import br.lcn.ragkb.service.ConflictCandidateService;
import br.lcn.ragkb.service.ConflictDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/conflicts")
@RequiredArgsConstructor
public class ConflictController {

    private final ConflictDetectionService conflictDetectionService;
    private final ConflictCandidateService conflictCandidateService;
    private final ConflictCandidateRepository conflictRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConflictCandidate> listAll() {
        return conflictRepository.findAll();
    }

    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConflictCandidate> scan() {
        conflictDetectionService.scan();
        return conflictRepository.findAll();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConflictCandidate updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody ConflictActionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return conflictCandidateService.updateStatus(id, request, auth.getName());
    }
}