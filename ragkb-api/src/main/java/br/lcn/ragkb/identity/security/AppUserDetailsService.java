package br.lcn.ragkb.identity.security;







import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.identity.AppRole;
import br.lcn.ragkb.identity.AppUser;
import br.lcn.ragkb.identity.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + username));
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(AppRole::parse)
                        .map(AppRole::simpleName)
                        .toArray(String[]::new))
                .disabled(!user.isEnabled())
                .build();
    }
}