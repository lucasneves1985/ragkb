package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.AnswerResponse;
import br.lcn.ragkb.dto.AskRequest;
import br.lcn.ragkb.service.QueryService;
import br.lcn.ragkb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;
    private final UserService userService;

    @PostMapping
    public AnswerResponse ask(@Valid @RequestBody AskRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String userSector = userService.findByUsername(auth.getName());
        return queryService.ask(request.question(), request.conversationId(), roles, auth.getName(), userSector);
    }
}