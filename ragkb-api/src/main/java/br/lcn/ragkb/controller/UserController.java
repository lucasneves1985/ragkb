package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.CreateUserRequest;
import br.lcn.ragkb.dto.ChangeOwnPasswordRequest;
import br.lcn.ragkb.dto.UpdateOwnProfileRequest;
import br.lcn.ragkb.dto.UpdateUserRequest;
import br.lcn.ragkb.dto.UserResponse;
import br.lcn.ragkb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getOwnProfile(Authentication auth) {
        return userService.getOwnProfile(auth.getName());
    }

    @PutMapping("/me")
    public UserResponse updateOwnProfile(@Valid @RequestBody UpdateOwnProfileRequest request, Authentication auth) {
        return userService.updateOwnProfile(auth.getName(), request);
    }

    @PutMapping("/me/password")
    public void changeOwnPassword(@Valid @RequestBody ChangeOwnPasswordRequest request, Authentication auth) {
        userService.changeOwnPassword(auth.getName(), request);
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public UserResponse updateOwnAvatar(@RequestPart("file") MultipartFile file, Authentication auth) {
        return userService.updateOwnAvatar(auth.getName(), file);
    }

    @DeleteMapping("/me/avatar")
    public UserResponse removeOwnAvatar(Authentication auth) {
        return userService.removeOwnAvatar(auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listAll() {
        return userService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
