package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.CreateSectorRequest;
import br.lcn.ragkb.dto.SectorResponse;
import br.lcn.ragkb.service.SectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    @GetMapping
    public List<SectorResponse> listAll() {
        return sectorService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SectorResponse create(@Valid @RequestBody CreateSectorRequest request) {
        return sectorService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        sectorService.delete(id);
    }
}