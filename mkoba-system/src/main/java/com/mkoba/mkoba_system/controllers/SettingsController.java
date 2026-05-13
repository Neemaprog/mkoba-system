package com.mkoba.mkoba_system.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class SettingsController {

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        try {
            System.out.println("🔍 DEBUG: Loading settings page");
            
            // Add current settings (you can customize these)
            model.addAttribute("systemName", "M-Koba System");
            model.addAttribute("systemVersion", "1.0.0");
            model.addAttribute("databaseUrl", "jdbc:postgresql://localhost:5432/m_koba");
            model.addAttribute("maxFileSize", "10MB");
            model.addAttribute("sessionTimeout", "30");
            model.addAttribute("emailNotifications", "true");
            model.addAttribute("smsNotifications", "false");
            model.addAttribute("backupFrequency", "daily");
            model.addAttribute("maintenanceMode", "false");
            
            return "admin/settings";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading settings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading settings: " + e.getMessage());
            return "admin/settings";
        }
    }

    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam(value = "systemName", required = false) String systemName,
            @RequestParam(value = "maxFileSize", required = false) String maxFileSize,
            @RequestParam(value = "sessionTimeout", required = false) String sessionTimeout,
            @RequestParam(value = "emailNotifications", required = false) String emailNotifications,
            @RequestParam(value = "smsNotifications", required = false) String smsNotifications,
            @RequestParam(value = "backupFrequency", required = false) String backupFrequency,
            @RequestParam(value = "maintenanceMode", required = false) String maintenanceMode,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔍 DEBUG: Saving settings");
            System.out.println("  - System Name: " + systemName);
            System.out.println("  - Max File Size: " + maxFileSize);
            System.out.println("  - Session Timeout: " + sessionTimeout);
            System.out.println("  - Email Notifications: " + emailNotifications);
            System.out.println("  - SMS Notifications: " + smsNotifications);
            System.out.println("  - Backup Frequency: " + backupFrequency);
            System.out.println("  - Maintenance Mode: " + maintenanceMode);
            
            // Here you would typically save these to a database or configuration file
            // For now, we'll just show a success message
            
            redirectAttributes.addFlashAttribute("success", "Mipangilio imesasishwa kwa mafanikio!");
            return "redirect:/admin/settings";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error saving settings: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Imeshindikana kusasisha mipangilio: " + e.getMessage());
            return "redirect:/admin/settings";
        }
    }
}
