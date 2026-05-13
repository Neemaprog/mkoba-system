package com.mkoba.mkoba_system.services;

import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSetupService implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        createAdminUser();
    }
    
    private void createAdminUser() {
        // Check if admin user already exists
        if (userRepository.existsByEmail("admin@mkoba.com")) {
            return;
        }
        
        // Create admin user without group for now
        User admin = new User();
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEmail("admin@mkoba.com");
        admin.setPhoneNumber("+255 700 000 000");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(User.UserRole.ADMIN); // Use ADMIN role
        admin.setGroup(null); // No group initially
        
        userRepository.save(admin);
        System.out.println("✅ Admin user created successfully!");
        System.out.println("📧 Email: admin@mkoba.com");
        System.out.println("🔑 Password: admin123");
        System.out.println("👤 Role: ADMIN");
    }
}
