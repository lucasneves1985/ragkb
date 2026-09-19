package br.lcn.ragkb.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Reminder;
import br.lcn.ragkb.entity.ReminderStatus;

public interface ReminderRepository extends JpaRepository<Reminder, String> {

    List<Reminder> findTop20ByStatusOrderByRemindAtAsc(ReminderStatus status);

    List<Reminder> findTop20ByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
            ReminderStatus status, Instant remindAt);

    List<Reminder> findByUserIdAndStatusOrderByRemindAtDesc(Long userId, ReminderStatus status);

    Optional<Reminder> findByIdAndUserId(String id, Long userId);
}
