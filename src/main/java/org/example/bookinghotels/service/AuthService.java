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

        @Autowired
        private UserRepository userRepository;

        private final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
        private static final int MAX_ATTEMPT = 5;

        public String login(String email, String rawPassword) {
            // 1. Tìm user bằng Email
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

            // 2. Kiểm tra tài khoản đã bị khóa chưa
            if (user.getDeleteAt() != null && user.getDeleteAt()) {
                throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
            }

            // 3. Kiểm tra mật khẩu (So sánh thô - không băm)
            if (!user.getPassword().equals(rawPassword)) {
                // Nếu sai, tăng đếm số lần sai
                int attempts = loginAttempts.getOrDefault(email, 0) + 1;
                loginAttempts.put(email, attempts);

                if (attempts >= MAX_ATTEMPT) {
                    user.setDeleteAt(true); // Khóa tài khoản
                    userRepository.save(user);
                    throw new RuntimeException("Tài khoản đã bị khóa do nhập sai 5 lần!");
                }
                throw new RuntimeException("Sai mật khẩu! Còn " + (MAX_ATTEMPT - attempts) + " lần thử.");
            }

            // 4. Nếu đăng nhập thành công
            loginAttempts.remove(email); // Reset số lần đếm

            // --- LOG 1: Kiểm tra danh sách Role lấy từ DB ---
            System.out.println("DEBUG: User roles size: " + user.getRoles().size());
            user.getRoles().forEach(r -> System.out.println("DEBUG: Role found: " + r.getRoleName()));
            // Lấy danh sách Role từ database và chuyển thành Authority
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleName())) // RoleName khớp với DB (ADMIN)
                    .collect(Collectors.toList());
// --- LOG 2: Kiểm tra danh sách quyền đã nạp ---
            System.out.println("DEBUG: Authorities list: " + authorities);
            // Tạo đối tượng Authentication
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

            // Nạp vào SecurityContext để Spring Security nhận diện được user này ở các request sau
            SecurityContextHolder.getContext().setAuthentication(auth);

// Thêm đoạn này vào cuối hàm login (sau SecurityContextHolder.getContext().setAuthentication(auth);)

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return "SUCCESS_TOKEN_123";

        }

    }