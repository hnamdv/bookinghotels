    package org.example.bookinghotels.service;
    import jakarta.servlet.http.HttpSession;
    import org.example.bookinghotels.entity.User;
    import org.example.bookinghotels.repository.UserRepository;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.web.context.request.RequestContextHolder;
    import org.springframework.web.context.request.ServletRequestAttributes;

    import java.util.List;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.stream.Collectors;

    @Service
    public class AuthService {
        @Autowired private UserRepository userRepository;
        @Autowired private PasswordEncoder passwordEncoder;

        private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
        private static final int MAX_ATTEMPT = 5;

        public String login(String identifier, String rawPassword) {
            // Kiểm tra đầu vào
            if (identifier == null || identifier.trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập Username hoặc Email!");
            }

            // 1. Tìm user (sử dụng .trim() để tránh lỗi thừa dấu cách)
            String cleanId = identifier.trim();
            User user = userRepository.findByEmail(cleanId)
                    .or(() -> userRepository.findByUsername(cleanId))
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            // 2. Kiểm tra tài khoản bị khóa
            if (Boolean.TRUE.equals(user.getDeleteAt())) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }

            // 3. Kiểm tra mật khẩu (Sử dụng matches của BCrypt)
            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
               int attempts = loginAttempts.getOrDefault(user.getEmail(), 0) + 1;
               loginAttempts.put(user.getEmail(), attempts);

              if (attempts >= MAX_ATTEMPT) {
                  user.setDeleteAt(true);
               userRepository.save(user);
                    throw new RuntimeException("Tài khoản đã bị khóa do nhập sai 5 lần!");
              }
                throw new RuntimeException("Sai mật khẩu! Còn " + (MAX_ATTEMPT - attempts) + " lần thử.");
            }

            // 4. Đăng nhập thành công: Reset đếm và thiết lập Security Context
            loginAttempts.remove(user.getEmail());
// CHUYỂN QUYỀN LỢI ĐỂ SPRING CAP PHEP
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

            // 5. Cấu hình Session
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(true);

            // Lưu thông tin để hiển thị ở sidebar
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("fullName", (user.getName() != null) ? user.getName() : user.getUsername());

            String roleDisplay = (user.getRoles() != null && !user.getRoles().isEmpty())
                    ? user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.joining(", "))
                    : "Nhân viên";
            session.setAttribute("role", roleDisplay);

            return "SUCCESS";
        }
    }