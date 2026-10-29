package br.lcn.ragkb.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.lcn.ragkb.dto.*;
import br.lcn.ragkb.service.CertificateMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/certificate-monitors") @RequiredArgsConstructor
public class CertificateMonitorController {
 private final CertificateMonitorService service;
 @GetMapping public List<CertificateMonitorDto> list(Authentication a){return service.list(a.getName());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public CertificateMonitorDto create(@Valid @RequestBody CertificateMonitorRequest r,Authentication a){return service.create(r,a.getName());}
 @PutMapping("/{id}") public CertificateMonitorDto update(@PathVariable String id,@Valid @RequestBody CertificateMonitorRequest r,Authentication a){return service.update(id,r,a.getName());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id,Authentication a){service.delete(id,a.getName());}
 @GetMapping("/{id}/history") public List<CertificateCheckDto> history(@PathVariable String id,Authentication a){return service.history(id,a.getName());}
 @PostMapping("/{id}/check") public void check(@PathVariable String id,Authentication a){service.checkNow(id,a.getName());}
}
