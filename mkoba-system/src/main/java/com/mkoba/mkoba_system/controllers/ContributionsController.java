package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ContributionsController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/contributions")
    public String contributions(
            @RequestParam(value = "cycle", defaultValue = "current") String cycle,
            @RequestParam(value = "group", defaultValue = "all") String groupId,
            @RequestParam(value = "status", defaultValue = "all") String status,
            Model model) {
        
        try {
            System.out.println("🔍 DEBUG: Loading contributions page");
            System.out.println("🔍 DEBUG: Cycle: " + cycle + ", Group: " + groupId + ", Status: " + status);

            // Get all groups for dropdown
            List<Group> groups = groupRepository.findAll();
            model.addAttribute("groups", groups);

            // Get all users with their contributions
            List<User> users = userRepository.findAll();
            
            // Filter by group if specified
            if (!groupId.equals("all")) {
                try {
                    Long groupIdLong = Long.parseLong(groupId);
                    users = users.stream()
                        .filter(user -> user.getGroup() != null && user.getGroup().getId().equals(groupIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    System.out.println("🔍 DEBUG: Invalid group ID format: " + groupId);
                }
            }

            // Calculate contribution statistics
            double totalCollection = 0.0;
            int totalMembers = users.size();
            
            for (User user : users) {
                if (user.getGroup() != null) {
                    double monthlyContribution = user.getGroup().getMonthlyContribution();
                    
                    // Calculate actual paid amount (simplified for now)
                    double paidAmount = monthlyContribution; // Assume fully paid for demo
                    double balance = monthlyContribution - paidAmount;
                    
                    // Apply status filter
                    String userStatus = balance <= 0 ? "Paid" : balance > 0 ? "Pending" : "Late";
                    
                    // Only include if matches status filter
                    if (status.equals("all") || userStatus.equals(status)) {
                        totalCollection += paidAmount;
                        
                        // Add calculated values to user object for display
                        user.setPaidAmount(paidAmount);
                        user.setBalance(balance);
                        user.setDueDate(LocalDate.now().plusDays(7)); // Due in 7 days
                        user.setContributionStatus(userStatus);
                    }
                }
            }

            // Add data to model
            model.addAttribute("users", users);
            model.addAttribute("totalCollection", totalCollection);
            model.addAttribute("totalMembers", totalMembers);
            model.addAttribute("selectedCycle", cycle);
            model.addAttribute("selectedGroup", groupId);
            model.addAttribute("selectedStatus", status);

            // Current month/year for display
            LocalDate currentDate = LocalDate.now();
            String currentMonth = currentDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            model.addAttribute("currentMonth", currentMonth);

            System.out.println("🔍 DEBUG: Found " + users.size() + " users");
            System.out.println("🔍 DEBUG: Total collection: " + totalCollection);

            return "admin/contributions";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading contributions: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading contributions: " + e.getMessage());
            return "admin/contributions";
        }
    }
}
