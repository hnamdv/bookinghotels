package org.example.bookinghotels.Controller;

import org.example.bookinghotels.entity.User;
import org.example.bookinghotels.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users") // Sửa từ /api/users thành /api/admin/users
public class UserController {
    @Autowired private UserService userService;
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

        @GetMapping
        public ResponseEntity<List<User>> listUsers() {
            return ResponseEntity.ok(userService.getAllUsers());
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
            userService.softDelete(id);
            return ResponseEntity.ok("Đã xóa khách hàng thành công");
        }

}