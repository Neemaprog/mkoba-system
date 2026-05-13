package com.mkoba.mkoba_system.services;

import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(User user) {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Barua pepe tayari imetumika");
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default role
        if (user.getRole() == null) {
            user.setRole(User.UserRole.MEMBER);
        }
        
        // Save user
        return userRepository.save(user);
    }
    
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
