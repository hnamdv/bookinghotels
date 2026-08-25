package org.example.bookinghotels.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cấp danh sách quyền hiện tại cho toàn bộ template Thymeleaf.
 * Dùng th:if thay vì sec:authorize để menu luôn hoạt động ổn định
 * kể cả khi phiên bản thymeleaf-extras thay đổi.
 */
@ControllerAdvice
public class GlobalAdminModelAdvice {

    @ModelAttribute("currentAuthorities")
    public Set<String> currentAuthorities(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
    }
}
