package br.lcn.ragkb.reminder;



import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, String> {

    List<Reminder> findTop20ByStatusOrderByRemindAtAsc(ReminderStatus status);

    List<Reminder> findTop20ByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
            ReminderStatus status, Instant remindAt);

    List<Reminder> findByUserIdAndStatusOrderByRemindAtDesc(Long userId, ReminderStatus status);

    Optional<Reminder> findByIdAndUserId(String id, Long userId);
}
