package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class MembersController {
    
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    
    public MembersController(UserRepository userRepository, GroupRepository groupRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping("/members")
    public String membersPage(Model model) {
        List<User> members = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("members", members);
        return "admin/members";
    }
    
    @GetMapping("/members/add")
    public String addMemberPage(Model model) {
        List<Group> groups = groupRepository.findAll();
        model.addAttribute("groups", groups);
        return "admin/add-member";
    }
    
    @PostMapping("/members/add")
    public String addMember(@RequestParam String firstName,
                         @RequestParam String lastName,
                         @RequestParam String email,
                         @RequestParam String phoneNumber,
                         @RequestParam String role,
                         @RequestParam Long groupId,
                         @RequestParam(required = false) String address,
                         @RequestParam String password) {
        
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            return "redirect:/admin/members?error=email_exists";
        }
        
        // Get group
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return "redirect:/admin/members?error=invalid_group";
        }
        
        // Create new member
        User member = new User();
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        member.setRole(User.UserRole.valueOf(role));
        member.setGroup(group);
        member.setPassword(passwordEncoder.encode(password));
        if (address != null && !address.trim().isEmpty()) {
            // Note: You'll need to add address field to User entity if you want to store this
        }
        
        userRepository.save(member);
        return "redirect:/admin/members?success=member_added";
    }
    
    @GetMapping("/members/edit/{id}")
    public String editMemberPage(@PathVariable Long id, Model model) {
        User member = userRepository.findById(id).orElse(null);
        if (member == null) {
            return "redirect:/admin/members?error=member_not_found";
        }
        
        List<Group> groups = groupRepository.findAll();
        model.addAttribute("member", member);
        model.addAttribute("groups", groups);
        return "admin/edit-member";
    }
    
    @PostMapping("/members/edit/{id}")
    public String editMember(@PathVariable Long id,
                          @RequestParam String firstName,
                          @RequestParam String lastName,
                          @RequestParam String email,
                          @RequestParam String phoneNumber,
                          @RequestParam String role,
                          @RequestParam Long groupId) {
        
        User member = userRepository.findById(id).orElse(null);
        if (member == null) {
            return "redirect:/admin/members?error=member_not_found";
        }
        
        // Check if email is used by another user
        if (!member.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            return "redirect:/admin/members?error=email_exists";
        }
        
        // Get group
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return "redirect:/admin/members?error=invalid_group";
        }
        
        // Update member
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        member.setRole(User.UserRole.valueOf(role));
        member.setGroup(group);
        
        userRepository.save(member);
        return "redirect:/admin/members?success=member_updated";
    }
    
    @PostMapping("/members/toggle-status/{id}")
    public String toggleMemberStatus(@PathVariable Long id) {
        User member = userRepository.findById(id).orElse(null);
        if (member == null) {
            return "redirect:/admin/members?error=member_not_found";
        }
        
        // Toggle status between ACTIVE and INACTIVE
        String newStatus = "ACTIVE".equals(member.getStatus()) ? "INACTIVE" : "ACTIVE";
        member.setStatus(newStatus);
        
        userRepository.save(member);
        return "redirect:/admin/members?success=status_toggled";
    }
    
    @PostMapping("/members/delete/{id}")
    public String deleteMember(@PathVariable Long id) {
        User member = userRepository.findById(id).orElse(null);
        if (member == null) {
            return "redirect:/admin/members?error=member_not_found";
        }
        
        userRepository.delete(member);
        return "redirect:/admin/members?success=member_deleted";
    }
}
