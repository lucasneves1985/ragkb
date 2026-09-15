package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.ArticleImageUploadResponse;
import br.lcn.ragkb.service.ArticleImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/articles/images")
@RequiredArgsConstructor
public class ArticleImageController {

    private final ArticleImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleImageUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return imageService.store(file);
    }
}