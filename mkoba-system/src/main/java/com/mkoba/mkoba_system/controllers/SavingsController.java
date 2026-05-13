package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Savings;
import com.mkoba.mkoba_system.entities.Savings.SavingsType;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.SavingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SavingsController {

    @Autowired
    private SavingsRepository savingsRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/savings")
    public String savings(
            @RequestParam(value = "date", defaultValue = "current") String date,
            @RequestParam(value = "group", defaultValue = "all") String groupId,
            @RequestParam(value = "member", defaultValue = "all") String memberId,
            Model model) {
        
        try {
            System.out.println("🔍 DEBUG: Loading savings page");
            System.out.println("🔍 DEBUG: Date: " + date + ", Group: " + groupId + ", Member: " + memberId);

            // Get all groups and users for dropdowns
            List<Group> groups = groupRepository.findAll();
            List<User> users = userRepository.findAll();
            model.addAttribute("groups", groups);
            model.addAttribute("users", users);

            // Get all savings
            List<Savings> savings = savingsRepository.findAll();
            
            // Apply filters
            if (!groupId.equals("all")) {
                try {
                    Long groupIdLong = Long.parseLong(groupId);
                    savings = savings.stream()
                        .filter(s -> s.getUser() != null && s.getUser().getGroup() != null && s.getUser().getGroup().getId().equals(groupIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    System.out.println("🔍 DEBUG: Invalid group ID format: " + groupId);
                }
            }

            if (!memberId.equals("all")) {
                try {
                    Long memberIdLong = Long.parseLong(memberId);
                    savings = savings.stream()
                        .filter(s -> s.getUser() != null && s.getUser().getId().equals(memberIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    System.out.println("🔍 DEBUG: Invalid member ID format: " + memberId);
                }
            }

            if (!date.equals("current")) {
                try {
                    LocalDate filterDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    savings = savings.stream()
                        .filter(s -> s.getContributionDate() != null && s.getContributionDate().toLocalDate().equals(filterDate))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    System.out.println("🔍 DEBUG: Invalid date format: " + date);
                }
            }

            // Calculate total savings
            double totalSavings = savings.stream()
                .filter(s -> s.getType() == null || 
                           s.getType().equals(SavingsType.MONTHLY_CONTRIBUTION) || 
                           s.getType().equals(SavingsType.ADDITIONAL_SAVINGS))
                .mapToDouble(Savings::getAmount)
                .sum();

            // Add data to model
            model.addAttribute("savings", savings);
            model.addAttribute("totalSavings", totalSavings);
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedGroup", groupId);
            model.addAttribute("selectedMember", memberId);

            // Current month/year for display
            LocalDate currentDate = LocalDate.now();
            String currentMonth = currentDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            model.addAttribute("currentMonth", currentMonth);

            System.out.println("🔍 DEBUG: Found " + savings.size() + " savings records");
            System.out.println("🔍 DEBUG: Total savings: " + totalSavings);
            
            // Debug: Print all savings details
            System.out.println("🔍 DEBUG: All savings records:");
            savings.forEach(s -> {
                System.out.println("  - ID: " + s.getId() + 
                                 ", Amount: " + s.getAmount() + 
                                 ", Type: " + s.getType() + 
                                 ", Date: " + s.getContributionDate() + 
                                 ", User: " + (s.getUser() != null ? s.getUser().getFirstName() : "NULL"));
            });

            return "admin/savings";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading savings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading savings: " + e.getMessage());
            return "admin/savings";
        }
    }
    
    @PostMapping("/admin/savings/statement")
    @ResponseBody
    public Map<String, Object> getSavingsStatement(@RequestParam Long savingsId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Find savings record
            Savings savings = savingsRepository.findById(savingsId).orElse(null);
            if (savings == null) {
                response.put("success", false);
                response.put("message", "Savings record not found");
                return response;
            }
            
            // Create statement data
            Map<String, Object> statement = new HashMap<>();
            statement.put("id", savings.getId());
            statement.put("amount", savings.getAmount());
            statement.put("contributionDate", savings.getContributionDate());
            statement.put("description", savings.getDescription());
            statement.put("type", savings.getType());
            statement.put("user", savings.getUser().getFirstName() + " " + savings.getUser().getLastName());
            statement.put("group", savings.getUser().getGroup().getName());
            
            response.put("success", true);
            response.put("statement", statement);
            response.put("message", "Statement retrieved successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error retrieving statement: " + e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/admin/savings/approve")
    @ResponseBody
    public Map<String, Object> approveSavings(@RequestParam Long savingsId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Find savings record
            Savings savings = savingsRepository.findById(savingsId).orElse(null);
            if (savings == null) {
                response.put("success", false);
                response.put("message", "Savings record not found");
                return response;
            }
            
            // Update status (assuming you have a status field)
            // savings.setStatus("APPROVED");
            savingsRepository.save(savings);
            
            response.put("success", true);
            response.put("message", "Savings approved successfully");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error approving savings: " + e.getMessage());
        }
        return response;
    }
}
