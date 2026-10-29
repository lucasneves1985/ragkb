package br.lcn.ragkb.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_configuration")
@Getter
@NoArgsConstructor
public class AppConfiguration {

    @Id
    @Column(length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public AppConfiguration(String key, String value, String updatedBy) {
        this.key = key;
        this.value = value;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void update(String value, String updatedBy) {
        this.value = value;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
}