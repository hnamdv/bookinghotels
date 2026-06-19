package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Cho phép tất cả các nguồn (Frontend sau này) gọi API không bị lỗi CORS
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // 1. Lấy danh sách (GET)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {

        // 2. Nếu Role có ID, Spring Data JPA sẽ tự ánh xạ nếu quan hệ đúng
        return ResponseEntity.ok(userService.createUser(user));
    }

    // 3. Sửa thông tin (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    // 4. Xóa (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
    // Trong UserController.java
    @PutMapping("/restore/{id}")
    public ResponseEntity<Void> restoreUser(@PathVariable Integer id) {
        userService.restoreUser(id);
        return ResponseEntity.ok().build();
    }
}