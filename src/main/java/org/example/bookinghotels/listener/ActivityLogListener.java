package org.example.bookinghotels.listener;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.example.bookinghotels.entity.ActivityLog;
import org.example.bookinghotels.repository.ActivityLogRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * JPA Entity Listener — KHÔNG cần spring-boot-starter-aop.
 * Gắn @EntityListeners(ActivityLogListener.class) lên entity nào thì
 * entity đó tự động được ghi log mỗi khi tạo / sửa / xóa.
 *
 * Lưu ý: JPA tự khởi tạo class này bằng constructor rỗng (không qua Spring),
 * nên phải lấy bean qua ApplicationContext tĩnh (set từ ContextHolder bên dưới).
 */
public class ActivityLogListener {

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @PostPersist
    public void onCreate(Object entity) {
        log("CREATE", entity);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        log("UPDATE", entity);
    }

    @PostRemove
    public void onRemove(Object entity) {
        log("DELETE", entity);
    }

    private void log(String action, Object entity) {
        try {
            if (applicationContext == null) return;
            if (entity instanceof ActivityLog) return; // tránh log đè log

            ActivityLogRepository activityLogRepository =
                    applicationContext.getBean(ActivityLogRepository.class);
            String tableName = entity.getClass().getSimpleName();
            String idPart = "";
            try {
                Method getId = entity.getClass().getMethod("getId");
                Object id = getId.invoke(entity);
                idPart = " (id=" + id + ")";
            } catch (Exception ignored) {
            }

            ActivityLog activityLog = new ActivityLog();
            activityLog.setAction(action);
            activityLog.setTableName(tableName);
            String username = getCurrentUsername();
            activityLog.setDescription(action + " " + tableName + idPart + (username == null ? "" : " | user=" + username));
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setUser(null);

            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            System.err.println("[ActivityLogListener] Lỗi ghi log: " + e.getMessage());
        }
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            return auth.getName();
        } catch (Exception e) {
            return null;
        }
    }
}