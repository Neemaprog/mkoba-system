package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.User;
import com.mkoba.mkoba_system.entities.Group;
import com.mkoba.mkoba_system.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class RegistrationController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("groups", userService.getAllGroups());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user, 
                           BindingResult result, 
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("groups", userService.getAllGroups());
            return "register";
        }
        
        // Check if email already exists
        if (userService.emailExists(user.getEmail())) {
            model.addAttribute("emailError", "Barua pepe tayari imetumika");
            model.addAttribute("groups", userService.getAllGroups());
            return "register";
        }
        
        try {
            // Register new user
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Umefanikiwa kujisajili! Tafadhali ingia k kutumia akaunti yako.");
            return "redirect:/login";
            
        } catch (Exception e) {
            model.addAttribute("error", "Imeshindikana kujisajili: " + e.getMessage());
            model.addAttribute("groups", userService.getAllGroups());
            return "register";
        }
    }
}
