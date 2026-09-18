package br.lcn.ragkb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.BusinessRuleAttachment;

public interface BusinessRuleAttachmentRepository extends JpaRepository<BusinessRuleAttachment, String> {
    List<BusinessRuleAttachment> findByRuleId(String ruleId);
    Optional<BusinessRuleAttachment> findByRuleIdAndId(String ruleId, String id);
}