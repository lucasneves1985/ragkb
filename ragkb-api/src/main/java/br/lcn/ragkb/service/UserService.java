package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.CreateUserRequest;
import br.lcn.ragkb.dto.ChangeOwnPasswordRequest;
import br.lcn.ragkb.dto.UpdateUserRequest;
import br.lcn.ragkb.dto.UpdateOwnProfileRequest;
import br.lcn.ragkb.dto.UserResponse;
import br.lcn.ragkb.entity.AppRole;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Sector;
import br.lcn.ragkb.exception.EmailAlreadyInUseException;
import br.lcn.ragkb.exception.UserNotFoundException;
import br.lcn.ragkb.exception.UsernameAlreadyExistsException;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SectorRepository sectorRepository;
    private final AvatarImageService avatarImageService;

    @Transactional(readOnly = true)
    public UserResponse getOwnProfile(String username) {
        return toResponse(findByUsernameOrThrow(username));
    }

    @Transactional
    public UserResponse updateOwnProfile(String username, UpdateOwnProfileRequest request) {
        AppUser user = findByUsernameOrThrow(username);
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new EmailAlreadyInUseException(email);
        }
        user.updateProfile(request.fullName().trim(), email, user.getPhone());
        return toResponse(user);
    }

    @Transactional
    public void changeOwnPassword(String username, ChangeOwnPasswordRequest request) {
        AppUser user = findByUsernameOrThrow(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public UserResponse updateOwnAvatar(String username, MultipartFile file) {
        AppUser user = findByUsernameOrThrow(username);
        String previous = user.getAvatarUrl();
        String avatarUrl = avatarImageService.store(file);
        user.updateAvatarUrl(avatarUrl);
        if (previous != null) avatarImageService.delete(previous);
        return toResponse(user);
    }

    @Transactional
    public UserResponse removeOwnAvatar(String username) {
        AppUser user = findByUsernameOrThrow(username);
        String previous = user.getAvatarUrl();
        user.updateAvatarUrl(null);
        if (previous != null) avatarImageService.delete(previous);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        List<String> roles = normalizeRoles(request.roles());
        Sector sector = loadSector(request.sectorId());

        AppUser saved = userRepository.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                sector,
                roles,
                request.fullName().trim(),
                email,
                normalizePhone(request.phone())));

        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new EmailAlreadyInUseException(email);
        }

        user.updateProfile(request.fullName().trim(), email, normalizePhone(request.phone()));
        user.changeSector(loadSector(request.sectorId()));

        if (request.password() != null && !request.password().isBlank()) {
            user.changePassword(passwordEncoder.encode(request.password()));
        }

        if (request.roles() != null) {
            user.replaceRoles(normalizeRoles(request.roles()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        return toResponse(user);
    }

    private List<String> normalizeRoles(List<String> roles) {
        List<String> normalized = roles.stream()
                .map(AppRole::parse)
                .map(AppRole::simpleName)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("O usuário deve ter ao menos um papel de acesso.");
        }
        return normalized;
    }

    private Sector loadSector(Long sectorId) {
        return sectorRepository.findById(sectorId)
                .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado: " + sectorId));
    }

    private static String normalizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone.trim();
    }

    private static UserResponse toResponse(AppUser u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getPhone(),
                u.getRoles(),
                u.getSector().getId(),
                u.getSector().getName(),
                u.isEnabled());
    }

    private AppUser findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    // Uso já existente no fluxo de consulta/RAG: o setor do usuário é o filtro da base.
    // Assinatura e semântica inalteradas (1 usuário = 1 setor).
    public String findByUsername(String username) {
        return userRepository.findByUsername(username).map(u -> u.getSector().getName())
                .orElse(null);
    }
}
