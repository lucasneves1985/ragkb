package br.lcn.ragkb.certificate;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateCheckRepository extends JpaRepository<CertificateCheck,String> {
    List<CertificateCheck> findTop100ByMonitorIdOrderByCheckedAtDesc(String monitorId);
}
