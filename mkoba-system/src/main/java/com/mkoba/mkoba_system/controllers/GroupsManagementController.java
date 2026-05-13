package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.repositories.GroupRepository;
import com.mkoba.mkoba_system.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/groups-management")
public class GroupsManagementController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String groupsManagement(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String status,
                                 Model model) {
        try {
            List<Group> groups;

            // Apply filters
            if (search != null && !search.trim().isEmpty()) {
                groups = groupRepository.findByNameContainingIgnoreCase(search.trim());
            } else if (status != null && !status.trim().isEmpty()) {
                if ("active".equals(status)) {
                    groups = groupRepository.findByActive(true);
                } else if ("closed".equals(status) || "inactive".equals(status)) {
                    groups = groupRepository.findByActive(false);
                } else {
                    groups = groupRepository.findAll();
                }
            } else {
                groups = groupRepository.findAll();
            }

            // Calculate member counts for each group
            Map<Long, Long> memberCounts = groups.stream()
                    .collect(Collectors.toMap(
                            Group::getId,
                            group -> userRepository.countByGroupId(group.getId())
                    ));

            model.addAttribute("groups", groups);
            model.addAttribute("memberCounts", memberCounts);
            model.addAttribute("search", search);
            model.addAttribute("status", status);

            return "admin/groups-management";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading groups: " + e.getMessage());
            return "admin/groups-management";
        }
    }

    @GetMapping("/view/{id}")
    public String viewGroup(@PathVariable Long id, Model model) {
        try {
            System.out.println("🔍 DEBUG: Loading group with ID: " + id);
            
            Group group = groupRepository.findById(id).orElse(null);
            if (group == null) {
                System.out.println("❌ ERROR: Group not found with ID: " + id);
                model.addAttribute("error", "Group not found!");
                return "admin/view-group";
            }

            System.out.println("✅ SUCCESS: Found group: " + group.getName());
            System.out.println("📊 Group Details:");
            System.out.println("  - Name: " + group.getName());
            System.out.println("  - Description: " + group.getDescription());
            System.out.println("  - Active: " + group.getActive());
            System.out.println("  - Monthly Contribution: " + group.getMonthlyContribution());

            // Get all members of the group
            List<User> members = userRepository.findByGroupId(id);
            System.out.println("👥 Found " + members.size() + " members in group");
            
            // Debug: Print member details
            for (User member : members) {
                System.out.println("  - Member: " + member.getFirstName() + " " + member.getLastName() + 
                                 " (" + member.getEmail() + ") - Role: " + member.getRole());
            }
            
            // Filter leaders (Chairperson, Secretary, Treasurer)
            List<User> leaders = members.stream()
                    .filter(user -> user.getRole() == User.UserRole.CHAIRPERSON || 
                                   user.getRole() == User.UserRole.SECRETARY || 
                                   user.getRole() == User.UserRole.TREASURER)
                    .collect(Collectors.toList());
            
            System.out.println("👔 Found " + leaders.size() + " leaders in group");

            model.addAttribute("group", group);
            model.addAttribute("members", members);
            model.addAttribute("leaders", leaders);
            // Note: contributions will be added later when we implement the savings system

            return "admin/view-group";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to load group details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading group details: " + e.getMessage());
            return "admin/view-group";
        }
    }

    @GetMapping("/edit/{id}")
    public String editGroup(@PathVariable Long id, Model model) {
        try {
            Group group = groupRepository.findById(id).orElse(null);
            if (group == null) {
                model.addAttribute("error", "Group not found!");
                return "admin/edit-group";
            }

            model.addAttribute("group", group);
            return "admin/edit-group";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading group for editing: " + e.getMessage());
            return "admin/edit-group";
        }
    }

    @PostMapping("/edit")
    public String saveGroup(@ModelAttribute Group group, 
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        try {
            // Validate required fields
            if (group.getName() == null || group.getName().trim().isEmpty()) {
                result.rejectValue("name", "Jina la kikundi linahitajika!");
                return "admin/edit-group";
            }
            
            if (group.getDescription() == null || group.getDescription().trim().isEmpty()) {
                result.rejectValue("description", "Maelezo ya kikundi yanahitajika!");
                return "admin/edit-group";
            }
            
            if (group.getMeetingFrequency() == null || group.getMeetingFrequency().trim().isEmpty()) {
                result.rejectValue("meetingFrequency", "Mkutano wa kukutana unahitajika!");
                return "admin/edit-group";
            }
            
            if (group.getMonthlyContribution() == null || group.getMonthlyContribution() <= 0) {
                result.rejectValue("monthlyContribution", "Mchango wa mwezi unahitajika!");
                return "admin/edit-group";
            }
            
            if (group.getMaxLoanAmount() == null || group.getMaxLoanAmount() <= 0) {
                result.rejectValue("maxLoanAmount", "Mkopo mzuri unahitajika!");
                return "admin/edit-group";
            }
            
            if (group.getInterestRate() == null || group.getInterestRate() < 0 || group.getInterestRate() > 50) {
                result.rejectValue("interestRate", "Riba inahitajika!");
                return "admin/edit-group";
            }

            // Check if group name already exists (for new groups)
            if (group.getId() == null) {
                if (groupRepository.existsByName(group.getName().trim())) {
                    result.rejectValue("name", "Jina la kikundi tayari limekuwa!");
                    return "admin/edit-group";
                }
            }

            groupRepository.save(group);
            
            if (group.getId() == null) {
                redirectAttributes.addFlashAttribute("success", "Kikundi kimeundwa kwa mafanikio!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Maelezo ya kikundi yamesashejwa kwa mafanikio!");
            }
            
            return "redirect:/admin/groups-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kuhifadhi kikundi: " + e.getMessage());
            return "redirect:/admin/groups-management";
        }
    }

    @GetMapping("/add")
    public String addGroup(Model model) {
        model.addAttribute("group", new Group());
        return "admin/edit-group";
    }

    @PostMapping("/delete/{id}")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Group group = groupRepository.findById(id).orElse(null);
            if (group != null) {
                // Check if group has members
                long memberCount = userRepository.countByGroupId(id);
                if (memberCount > 0) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Cannot delete group '" + group.getName() + "' because it has " + memberCount + " members.");
                } else {
                    groupRepository.delete(group);
                    redirectAttributes.addFlashAttribute("success", 
                        "Group '" + group.getName() + "' deleted successfully!");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Group not found!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting group: " + e.getMessage());
        }
        return "redirect:/admin/groups-management";
    }

    @PostMapping("/reopen/{id}")
    public String reopenGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Group group = groupRepository.findById(id).orElse(null);
            if (group != null) {
                group.setActive(true);
                groupRepository.save(group);
                redirectAttributes.addFlashAttribute("success", 
                    "Group '" + group.getName() + "' reopened successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Group not found!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error reopening group: " + e.getMessage());
        }
        return "redirect:/admin/groups-management";
    }
}
