package br.lcn.ragkb.config;

import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(new AppUser("admin",
                    passwordEncoder.encode(adminPassword), List.of("ADMIN", "USER")));
        }
    }
}