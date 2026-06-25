package com.pagatu.coffee.service;

import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.event.TurnReminderEvent;
import com.pagatu.coffee.repository.UserGroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TurnReminderService {

    private static final int REMINDER_24H = 1;
    private static final int REMINDER_48H = 2;

    @Value("${spring.nats.subject.turn-reminder-subject}")
    private String turnReminderSubject;

    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final OutboxService outboxService;

    @Scheduled(fixedRateString = "${app.turn-reminder.poll-interval-ms:3600000}")
    @Transactional
    public void processTurnReminders() {
        List<UserGroupMembership> activeTurns = userGroupMembershipRepository.findAllWithActiveTurn();
        LocalDateTime now = LocalDateTime.now();

        for (UserGroupMembership membership : activeTurns) {
            if (membership.getTurnAssignedAt() == null) {
                membership.setTurnAssignedAt(now);
                membership.setReminderLevel(0);
                userGroupMembershipRepository.save(membership);
                continue;
            }

            int currentLevel = membership.getReminderLevel() != null ? membership.getReminderLevel() : 0;
            long hoursPending = java.time.Duration.between(membership.getTurnAssignedAt(), now).toHours();

            if (hoursPending >= 48 && currentLevel < REMINDER_48H) {
                sendReminder(membership, 48);
                membership.setReminderLevel(REMINDER_48H);
                userGroupMembershipRepository.save(membership);
            } else if (hoursPending >= 24 && currentLevel < REMINDER_24H) {
                sendReminder(membership, 24);
                membership.setReminderLevel(REMINDER_24H);
                userGroupMembershipRepository.save(membership);
            }
        }
    }

    private void sendReminder(UserGroupMembership membership, int hoursPending) {
        if (Boolean.FALSE.equals(membership.getCoffeeUser().getEmailTurnReminders())) {
            log.debug("Promemoria turno saltato per {} — notifiche email disabilitate",
                    membership.getCoffeeUser().getUsername());
            return;
        }

        TurnReminderEvent event = new TurnReminderEvent();
        event.setUsername(membership.getCoffeeUser().getUsername());
        event.setEmail(membership.getCoffeeUser().getEmail());
        event.setGroupName(membership.getGroup().getName());
        event.setHoursPending(hoursPending);

        outboxService.saveEvent(turnReminderSubject, event);
        log.info("Promemoria turno {}h inviato a {} per gruppo {}",
                hoursPending, event.getUsername(), event.getGroupName());
    }
}