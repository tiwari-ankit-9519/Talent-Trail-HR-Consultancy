package com.abchr.platform.attendance;

import com.abchr.platform.notification.NotificationService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AttendanceNotifier {

    static final String LEAVE = "leave";
    static final String CORRECTION = "correction";

    private static final String LEAVE_LINK_APPROVER = "/attendance/leave-requests";
    private static final String CORRECTION_LINK_APPROVER = "/attendance/team";
    private static final String OWNER_LINK = "/attendance";

    private final NotificationService notifications;
    private final UserRepository userRepository;

    AttendanceNotifier(NotificationService notifications, UserRepository userRepository) {
        this.notifications = notifications;
        this.userRepository = userRepository;
    }

    void leaveRequested(UUID ownerId, String what) {
        notifications.notifyUsers(leaveApprovers(), LEAVE, "New leave request", name(ownerId) + ": " + what,
                LEAVE_LINK_APPROVER);
    }

    void leaveDecided(UUID ownerId, boolean approved, String what) {
        notifications.notifyUsers(List.of(ownerId), LEAVE, approved ? "Leave approved" : "Leave rejected", what,
                OWNER_LINK);
        notifications.refresh(leaveApprovers(), LEAVE);
    }

    void leaveCancelled(UUID ownerId, String what) {
        if (!ownerId.equals(Actor.current().id())) {
            notifications.notifyUsers(List.of(ownerId), LEAVE, "Leave cancelled by manager", what, OWNER_LINK);
        }
        notifications.refresh(leaveApprovers(), LEAVE);
    }

    void correctionRequested(UUID ownerId, String what) {
        notifications.notifyUsers(correctionApprovers(), CORRECTION, "New attendance correction",
                name(ownerId) + ": " + what, CORRECTION_LINK_APPROVER);
    }

    void correctionDecided(UUID ownerId, boolean approved, String what) {
        notifications.notifyUsers(List.of(ownerId), CORRECTION,
                approved ? "Correction approved" : "Correction rejected", what, OWNER_LINK);
        notifications.refresh(correctionApprovers(), CORRECTION);
    }

    private List<UUID> leaveApprovers() {
        return notifications.usersWithAnyPermission("ATTENDANCE_APPROVE", "ATTENDANCE_MANAGE");
    }

    private List<UUID> correctionApprovers() {
        return notifications.usersWithAnyPermission("ATTENDANCE_MANAGE");
    }

    private String name(UUID userId) {
        return userRepository.findById(userId).map(AppUser::getFullName).orElse("Someone");
    }
}
