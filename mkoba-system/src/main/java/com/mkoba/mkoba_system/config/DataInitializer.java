package com.mkoba.mkoba_system.config;

import com.mkoba.mkoba_system.entities.*;
import com.mkoba.mkoba_system.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SavingsRepository savingsRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Initialize data only if tables are empty
        if (groupRepository.count() == 0) {
            System.out.println("🚀 Initializing M-Koba System Data...");
            System.out.println("📊 Creating sample groups...");
            initializeGroups();
            
            System.out.println("👑 Creating admin user...");
            initializeAdminUser();
            
            System.out.println("👥 Creating sample users...");
            initializeUsers();
            
            System.out.println("✅ M-Koba System initialization complete!");
        }
    }
    
    private void initializeGroups() {
        Group group1 = new Group();
        group1.setName("Jumuiya ya Wanawake Kijijini");
        group1.setDescription("Kikundi cha wanawake kinachoendeleza miradi ya pamoja");
        group1.setMeetingFrequency("Kila Jumamosi");
        group1.setMonthlyContribution(50000.0);
        group1.setMaxLoanAmount(500000.0);
        group1.setInterestRate(10.0);
        
        Group group2 = new Group();
        group2.setName("Wafanyabiashara Vijijini");
        group2.setDescription("Kikundi cha wafanyabiashara wadogo");
        group2.setMeetingFrequency("Kila Jumapili");
        group2.setMonthlyContribution(100000.0);
        group2.setMaxLoanAmount(1000000.0);
        group2.setInterestRate(8.0);
        
        groupRepository.saveAll(Arrays.asList(group1, group2));
    }
    
    private void initializeAdminUser() {
        try {
            // Check if admin user already exists
            if (!userRepository.existsByEmail("admin@mkoba.com")) {
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setEmail("admin@mkoba.com");
                admin.setPhoneNumber("+255 700 000 000");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(User.UserRole.ADMIN);
                // Admin doesn't need a group
                userRepository.save(admin);
                System.out.println("✅ Admin user created successfully!");
                System.out.println("📧 Email: admin@mkoba.com");
                System.out.println("🔑 Password: admin123");
                System.out.println("👤 Role: ADMIN");
            } else {
                System.out.println("ℹ️ Admin user already exists: admin@mkoba.com");
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating admin user: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeUsers() {
        List<Group> groups = groupRepository.findAll();
        Group group1 = groups.get(0);
        Group group2 = groups.get(1);
        
        // Users for group 1
        User user1 = new User();
        user1.setFirstName("Mariam");
        user1.setLastName("Juma");
        user1.setEmail("mariam.juma@example.com");
        user1.setPhoneNumber("+255 712 345 678");
        user1.setPassword(passwordEncoder.encode("password123"));
        user1.setGroup(group1);
        user1.setRole(User.UserRole.CHAIRPERSON);
        
        User user2 = new User();
        user2.setFirstName("Fatuma");
        user2.setLastName("Mussa");
        user2.setEmail("fatuma.mussa@example.com");
        user2.setPhoneNumber("+255 713 456 789");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2.setGroup(group1);
        user2.setRole(User.UserRole.SECRETARY);
        
        User user3 = new User();
        user3.setFirstName("Aisha");
        user3.setLastName("Khamis");
        user3.setEmail("aisha.khamis@example.com");
        user3.setPhoneNumber("+255 714 567 890");
        user3.setPassword(passwordEncoder.encode("password123"));
        user3.setGroup(group1);
        user3.setRole(User.UserRole.TREASURER);
        
        User user4 = new User();
        user4.setFirstName("Zainab");
        user4.setLastName("Ali");
        user4.setEmail("zainab.ali@example.com");
        user4.setPhoneNumber("+255 715 678 901");
        user4.setPassword(passwordEncoder.encode("password123"));
        user4.setGroup(group1);
        user4.setRole(User.UserRole.MEMBER);
        
        // Users for group 2
        User user5 = new User();
        user5.setFirstName("Hassan");
        user5.setLastName("Makame");
        user5.setEmail("hassan.makame@example.com");
        user5.setPhoneNumber("+255 716 789 012");
        user5.setPassword(passwordEncoder.encode("password123"));
        user5.setGroup(group2);
        user5.setRole(User.UserRole.CHAIRPERSON);
        
        User user6 = new User();
        user6.setFirstName("Said");
        user6.setLastName("Omar");
        user6.setEmail("said.omar@example.com");
        user6.setPhoneNumber("+255 717 890 123");
        user6.setPassword(passwordEncoder.encode("password123"));
        user6.setGroup(group2);
        user6.setRole(User.UserRole.MEMBER);
        
        userRepository.saveAll(Arrays.asList(user1, user2, user3, user4, user5, user6));
    }
}
