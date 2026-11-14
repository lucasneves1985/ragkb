package br.lcn.ragkb.businessrule;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRuleAttachmentRepository extends JpaRepository<BusinessRuleAttachment, String> {
    List<BusinessRuleAttachment> findByRuleId(String ruleId);
    Optional<BusinessRuleAttachment> findByRuleIdAndId(String ruleId, String id);
}