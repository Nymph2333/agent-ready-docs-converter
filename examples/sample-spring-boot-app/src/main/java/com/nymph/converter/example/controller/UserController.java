package com.nymph.converter.example.controller;

import com.nymph.converter.example.model.User;
import com.nymph.converter.example.model.CreateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private List<User> users = new ArrayList<>();
    private Long nextId = 1L;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst();
        
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = new User(
            request.getUsername(),
            request.getEmail(),
            request.getFirstName(),
            request.getLastName()
        );
        user.setId(nextId++);
        user.setAge(request.getAge());
        
        users.add(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody CreateUserRequest request) {
        Optional<User> existingUser = users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst();
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setAge(request.getAge());
            
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean removed = users.removeIf(u -> u.getId().equals(id));
        
        if (removed) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String email) {
        List<User> result = users.stream()
            .filter(u -> username == null || u.getUsername().contains(username))
            .filter(u -> email == null || u.getEmail().contains(email))
            .toList();
        
        return ResponseEntity.ok(result);
    }
}