package br.lcn.ragkb.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import br.lcn.ragkb.entity.CertificateCheck;
public interface CertificateCheckRepository extends JpaRepository<CertificateCheck,String> {
    List<CertificateCheck> findTop100ByMonitorIdOrderByCheckedAtDesc(String monitorId);
}
