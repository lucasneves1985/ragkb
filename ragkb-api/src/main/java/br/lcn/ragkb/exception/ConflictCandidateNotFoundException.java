package br.lcn.ragkb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConflictCandidateNotFoundException extends RuntimeException {
    public ConflictCandidateNotFoundException(Long id) {
        super("Candidato a conflito não encontrado: " + id);
    }
}