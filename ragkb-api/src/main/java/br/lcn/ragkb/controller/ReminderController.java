package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.ReminderCreateRequest;
import br.lcn.ragkb.dto.ReminderDto;
import br.lcn.ragkb.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderDto create(@Valid @RequestBody ReminderCreateRequest request, Authentication auth) {
        return service.create(request.request(), auth.getName(), request.conversationId());
    }

    @GetMapping
    public List<ReminderDto> list(Authentication auth) {
        return service.listActive(auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String id, Authentication auth) {
        service.cancel(id, auth.getName());
    }
}
