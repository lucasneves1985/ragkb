package br.lcn.ragkb.certificate;


import java.util.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CertificateMonitorRepository extends JpaRepository<CertificateMonitor,String> {
    List<CertificateMonitor> findByCreatedByOrderByCreatedAtDesc(String username);
    List<CertificateMonitor> findByActiveTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<CertificateMonitor> findFirstById(String id);
    Optional<CertificateMonitor> findByIdAndCreatedBy(String id,String username);
}
