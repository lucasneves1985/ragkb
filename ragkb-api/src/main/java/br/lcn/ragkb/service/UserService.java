package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.CreateUserRequest;
import br.lcn.ragkb.dto.UserResponse;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Sector;
import br.lcn.ragkb.exception.InvalidRoleException;
import br.lcn.ragkb.exception.UsernameAlreadyExistsException;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.SectorRepository;
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
    private final SectorRepository sectorRepository;

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getRoles(), u.isEnabled()))
                .toList();
    }

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
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Setor não encontrado: " + request.sectorId()));
                        
        AppUser saved = userRepository.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                sector,
                roles));

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRoles(), saved.isEnabled());
    }


    public String findByUsername(String username) {
        return userRepository.findByUsername(username).map(u -> u.getSector().getName())
                .orElse(null);
    }
}