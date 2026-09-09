package br.lcn.ragkb.config;

import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Sector;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.SectorRepository;
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
    private final SectorRepository sectorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            Sector sector = sectorRepository.findById(Long.valueOf(1)).orElse(null);
            userRepository.save(new AppUser("admin",
                    passwordEncoder.encode(adminPassword), sector, List.of("ADMIN", "USER")));
        }
    }
}