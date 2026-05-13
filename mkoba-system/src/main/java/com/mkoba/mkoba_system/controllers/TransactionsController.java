package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Transaction;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import com.mkoba.mkoba_system.repositories.TransactionRepository;
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
public class TransactionsController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/transactions")
    public String transactions(
            @RequestParam(value = "date", defaultValue = "all") String date,
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "member", defaultValue = "all") String memberId,
            @RequestParam(value = "group", defaultValue = "all") String groupId,
            Model model) {
        
        try {
            System.out.println("🔍 DEBUG: Loading transactions page");
            System.out.println("🔍 DEBUG: Date: " + date + ", Type: " + type + ", Member: " + memberId + ", Group: " + groupId);

            // Get all groups and users for dropdowns
            List<Group> groups = groupRepository.findAll();
            List<User> users = userRepository.findAll();
            model.addAttribute("groups", groups);
            model.addAttribute("users", users);

            // Get all transactions
            List<Transaction> transactions = transactionRepository.findAll();
            
            // Apply filters
            if (!groupId.equals("all")) {
                try {
                    Long groupIdLong = Long.parseLong(groupId);
                    transactions = transactions.stream()
                        .filter(t -> t.getUser() != null && t.getUser().getGroup() != null && t.getUser().getGroup().getId().equals(groupIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    System.out.println("🔍 DEBUG: Invalid group ID format: " + groupId);
                }
            }

            if (!memberId.equals("all")) {
                try {
                    Long memberIdLong = Long.parseLong(memberId);
                    transactions = transactions.stream()
                        .filter(t -> t.getUser() != null && t.getUser().getId().equals(memberIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    System.out.println("🔍 DEBUG: Invalid member ID format: " + memberId);
                }
            }

            if (!type.equals("all")) {
                transactions = transactions.stream()
                    .filter(t -> t.getType() != null && t.getType().equals(type))
                    .collect(Collectors.toList());
            }

            if (!date.equals("all")) {
                try {
                    LocalDate filterDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    transactions = transactions.stream()
                        .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().toLocalDate().equals(filterDate))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    System.out.println("🔍 DEBUG: Invalid date format: " + date);
                }
            }

            // Calculate total transactions count
            int totalCount = transactions.size();

            // Add data to model
            model.addAttribute("transactions", transactions);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedType", type);
            model.addAttribute("selectedMember", memberId);
            model.addAttribute("selectedGroup", groupId);

            // Current date for display
            LocalDate currentDate = LocalDate.now();
            String currentMonth = currentDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            model.addAttribute("currentMonth", currentMonth);

            System.out.println("🔍 DEBUG: Found " + transactions.size() + " transactions");
            System.out.println("🔍 DEBUG: Total count: " + totalCount);

            return "admin/transactions";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading transactions: " + e.getMessage());
            return "admin/transactions";
        }
    }
}
