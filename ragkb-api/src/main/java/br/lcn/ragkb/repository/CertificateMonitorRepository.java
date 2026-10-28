package br.lcn.ragkb.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import br.lcn.ragkb.entity.CertificateMonitor;
public interface CertificateMonitorRepository extends JpaRepository<CertificateMonitor,String> {
    List<CertificateMonitor> findByCreatedByOrderByCreatedAtDesc(String username);
    List<CertificateMonitor> findByActiveTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<CertificateMonitor> findFirstById(String id);
    Optional<CertificateMonitor> findByIdAndCreatedBy(String id,String username);
}
