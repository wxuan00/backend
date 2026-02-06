package com.msp.backend.modules.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow Angular to access this
public class UserController {

    private final UserService userService;

    // GET http://localhost:8080/api/users
    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    // POST http://localhost:8080/api/users
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}