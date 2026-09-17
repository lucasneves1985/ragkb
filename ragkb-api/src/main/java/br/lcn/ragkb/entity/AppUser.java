package br.lcn.ragkb.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Imutável por regra de negócio: nunca alterado após a criação
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 50)
    private List<String> roles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false)
    private boolean enabled = true;

    public AppUser(String username, String password, Sector sector, List<String> roles,
                   String fullName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.sector = sector;
        this.roles = roles;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public void changeSector(Sector sector) {
        this.sector = sector;
    }

    public void updateProfile(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public void replaceRoles(List<String> roles) {
        this.roles = new ArrayList<>(roles);
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}