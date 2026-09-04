package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.CreateUserRequest;
import br.lcn.ragkb.dto.UserResponse;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.exception.InvalidRoleException;
import br.lcn.ragkb.exception.UsernameAlreadyExistsException;
import br.lcn.ragkb.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("USER", "ADMIN", "EDITOR");

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        List<String> roles = request.roles().stream()
                .map(String::toUpperCase)
                .toList();
        for (String role : roles) {
            if (!VALID_ROLES.contains(role)) {
                throw new InvalidRoleException(role);
            }
        }

        AppUser saved = userRepository.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                roles));

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRoles(), saved.isEnabled());
    }
}