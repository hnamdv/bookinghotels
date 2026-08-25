package org.example.bookinghotels.Controller;

import jakarta.servlet.http.HttpSession;
import org.example.bookinghotels.entity.Role;
import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('ROLE_USER')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id,
                                           @RequestBody User userDetails,
                                           HttpSession session) {
        // Lưu username cũ để nhận biết trường hợp đang sửa chính tài khoản đang đăng nhập.
        String oldUsername = userService.getUserById(id).getUsername();
        User updated = userService.updateUser(id, userDetails);

        refreshCurrentAuthenticationIfNeeded(oldUsername, updated, session);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<Void> restoreUser(@PathVariable Integer id) {
        userService.restoreUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Nếu người quản trị vừa sửa role/username của chính tài khoản đang đăng nhập,
     * cập nhật SecurityContext ngay lập tức để không phải logout/login lại.
     */
    private void refreshCurrentAuthenticationIfNeeded(String oldUsername, User updated, HttpSession session) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated() || !oldUsername.equals(current.getName())) {
            return;
        }

        List<SimpleGrantedAuthority> authorities = updated.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .toList();

        UsernamePasswordAuthenticationToken refreshed =
                new UsernamePasswordAuthenticationToken(updated.getUsername(), current.getCredentials(), authorities);
        refreshed.setDetails(current.getDetails());

        SecurityContextHolder.getContext().setAuthentication(refreshed);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        session.setAttribute("fullName", updated.getName() != null ? updated.getName() : updated.getUsername());
        session.setAttribute("role", updated.getRoles().stream()
                .map(Role::getRoleName)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("Nhân viên"));
    }
}
