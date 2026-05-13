package com.mkoba.mkoba_system.controllers;

import com.mkoba.mkoba_system.entities.*;
import com.mkoba.mkoba_system.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SavingsRepository savingsRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        try {
            System.out.println("🔍 DEBUG: Loading admin dashboard with real data");
            
            // Get real statistics from database
            List<User> users = userRepository.findAll();
            List<Group> groups = groupRepository.findAll();
            List<Savings> savings = savingsRepository.findAll();
            List<Loan> loans = loanRepository.findAll();
            List<Notification> notifications = notificationRepository.findAll();
            
            // Calculate real totals
            double totalSavings = savings.stream().mapToDouble(Savings::getAmount).sum();
            double totalLoans = loans.stream().mapToDouble(Loan::getAmount).sum();
            long pendingLoans = loans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.PENDING).count();
            long activeUsers = users.stream().filter(u -> "ACTIVE".equals(u.getStatus())).count();
            long unreadNotifications = notifications.stream().filter(n -> !n.getIsRead()).count();
            
            // Get recent activities
            List<Loan> recentLoans = loans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.APPROVED || l.getStatus() == Loan.LoanStatus.ACTIVE)
                .sorted((l1, l2) -> l2.getApplicationDate().compareTo(l1.getApplicationDate()))
                .limit(5)
                .toList();
            
            List<Savings> recentSavings = savings.stream()
                .sorted((s1, s2) -> s2.getContributionDate().compareTo(s1.getContributionDate()))
                .limit(5)
                .toList();
            
            System.out.println("🔍 DEBUG: Real stats - Users: " + users.size() + ", Groups: " + groups.size());
            System.out.println("🔍 DEBUG: Total Savings: " + totalSavings + ", Total Loans: " + totalLoans);
            System.out.println("🔍 DEBUG: Pending Loans: " + pendingLoans + ", Active Users: " + activeUsers);
            
            // Add real data to model
            model.addAttribute("adminName", "Administrator");
            model.addAttribute("totalUsers", users.size());
            model.addAttribute("totalGroups", groups.size());
            model.addAttribute("totalSavings", totalSavings);
            model.addAttribute("totalLoans", totalLoans);
            model.addAttribute("pendingLoans", pendingLoans);
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("unreadNotifications", unreadNotifications);
            model.addAttribute("recentLoans", recentLoans);
            model.addAttribute("recentSavings", recentSavings);
            
            return "admin/admin_dashboard";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "admin/admin_dashboard";
        }
    }
    
    @GetMapping("/reports")
    public String adminReports(@RequestParam(value = "type", defaultValue = "daily") String reportType, Model model) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate monthStart = today.withDayOfMonth(1);
            
            System.out.println("🔍 DEBUG: Loading admin reports page with type: " + reportType);
            System.out.println("🔍 DEBUG: Today is: " + today);
            System.out.println("🔍 DEBUG: Week start: " + weekStart);
            System.out.println("🔍 DEBUG: Month start: " + monthStart);
            
            // Get all data
            List<Savings> allSavings = savingsRepository.findAll();
            List<Loan> allLoans = loanRepository.findAll();
            List<User> allUsers = userRepository.findAll();
            
            System.out.println("🔍 DEBUG: Total savings in DB: " + allSavings.size());
            System.out.println("🔍 DEBUG: Total loans in DB: " + allLoans.size());
            System.out.println("🔍 DEBUG: Total users in DB: " + allUsers.size());
            
            // Daily Report (Today)
            List<Savings> dailySavings = allSavings.stream()
                .filter(s -> {
                    LocalDate savingDate = s.getContributionDate().toLocalDate();
                    boolean matches = savingDate.equals(today);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Daily saving - User: " + s.getUser().getFirstName() + 
                                          ", Amount: " + s.getAmount() + 
                                          ", Date: " + s.getContributionDate() +
                                          ", Time: " + s.getContributionDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            List<Loan> dailyLoans = allLoans.stream()
                .filter(l -> {
                    LocalDate loanDate = l.getApplicationDate().toLocalDate();
                    boolean matches = loanDate.equals(today);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Daily loan - User: " + l.getUser().getFirstName() + 
                                          ", Amount: " + l.getAmount() + 
                                          ", Date: " + l.getApplicationDate() +
                                          ", Time: " + l.getApplicationDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            double dailySavingsTotal = dailySavings.stream().mapToDouble(Savings::getAmount).sum();
            double dailyLoansTotal = dailyLoans.stream().mapToDouble(Loan::getAmount).sum();
            
            // Weekly Report (This Week)
            List<Savings> weeklySavings = allSavings.stream()
                .filter(s -> {
                    LocalDate savingDate = s.getContributionDate().toLocalDate();
                    boolean matches = !savingDate.isBefore(weekStart);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Weekly saving - User: " + s.getUser().getFirstName() + 
                                          ", Amount: " + s.getAmount() + 
                                          ", Date: " + s.getContributionDate() +
                                          ", Day: " + savingDate.getDayOfWeek());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            List<Loan> weeklyLoans = allLoans.stream()
                .filter(l -> {
                    LocalDate loanDate = l.getApplicationDate().toLocalDate();
                    boolean matches = !loanDate.isBefore(weekStart);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Weekly loan - User: " + l.getUser().getFirstName() + 
                                          ", Amount: " + l.getAmount() + 
                                          ", Date: " + l.getApplicationDate() +
                                          ", Day: " + loanDate.getDayOfWeek());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            double weeklySavingsTotal = weeklySavings.stream().mapToDouble(Savings::getAmount).sum();
            double weeklyLoansTotal = weeklyLoans.stream().mapToDouble(Loan::getAmount).sum();
            
            // Monthly Report (This Month)
            List<Savings> monthlySavings = allSavings.stream()
                .filter(s -> {
                    LocalDate savingDate = s.getContributionDate().toLocalDate();
                    boolean matches = !savingDate.isBefore(monthStart);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Monthly saving - User: " + s.getUser().getFirstName() + 
                                          ", Amount: " + s.getAmount() + 
                                          ", Date: " + s.getContributionDate() +
                                          ", Day: " + savingDate.getDayOfMonth());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            List<Loan> monthlyLoans = allLoans.stream()
                .filter(l -> {
                    LocalDate loanDate = l.getApplicationDate().toLocalDate();
                    boolean matches = !loanDate.isBefore(monthStart);
                    if (matches) {
                        System.out.println("🔍 DEBUG: Monthly loan - User: " + l.getUser().getFirstName() + 
                                          ", Amount: " + l.getAmount() + 
                                          ", Date: " + l.getApplicationDate() +
                                          ", Day: " + loanDate.getDayOfMonth());
                    }
                    return matches;
                })
                .collect(Collectors.toList());
            
            double monthlySavingsTotal = monthlySavings.stream().mapToDouble(Savings::getAmount).sum();
            double monthlyLoansTotal = monthlyLoans.stream().mapToDouble(Loan::getAmount).sum();
            
            // Add data to model
            model.addAttribute("adminName", "Administrator");
            model.addAttribute("currentDate", today.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            model.addAttribute("weekRange", weekStart.format(DateTimeFormatter.ofPattern("dd MMM")) + " - " + today.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            model.addAttribute("monthYear", today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            
            // Set report type flags
            model.addAttribute("dailyReport", "daily".equals(reportType));
            model.addAttribute("weeklyReport", "weekly".equals(reportType));
            model.addAttribute("monthlyReport", "monthly".equals(reportType));
            
            // Add current time for transactions with missing time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalDateTime currentTime = LocalDateTime.now();
            
            // Daily Report Data
            model.addAttribute("dailySavings", dailySavings);
            model.addAttribute("dailyLoans", dailyLoans);
            model.addAttribute("dailySavingsTotal", dailySavingsTotal);
            model.addAttribute("dailyLoansTotal", dailyLoansTotal);
            model.addAttribute("dailyTransactions", dailySavings.size() + dailyLoans.size());
            model.addAttribute("currentTime", currentTime);
            model.addAttribute("timeFormatter", timeFormatter);
            
            // Weekly Report Data
            model.addAttribute("weeklySavings", weeklySavings);
            model.addAttribute("weeklyLoans", weeklyLoans);
            model.addAttribute("weeklySavingsTotal", weeklySavingsTotal);
            model.addAttribute("weeklyLoansTotal", weeklyLoansTotal);
            model.addAttribute("weeklyTransactions", weeklySavings.size() + weeklyLoans.size());
            
            // Calculate weekly statistics
            double weeklyAvgSavings = weeklySavings.size() > 0 ? weeklySavingsTotal / 7 : 0;
            double weeklyAvgLoans = weeklyLoans.size() > 0 ? weeklyLoansTotal / 7 : 0;
            int daysInWeek = 7;
            double weeklyGrowthRate = dailySavingsTotal > 0 ? ((weeklySavingsTotal - dailySavingsTotal) / dailySavingsTotal) * 100 : 0;
            
            model.addAttribute("weeklyAvgSavings", weeklyAvgSavings);
            model.addAttribute("weeklyAvgLoans", weeklyAvgLoans);
            model.addAttribute("weeklyGrowthRate", weeklyGrowthRate);
            
            System.out.println("🔍 DEBUG: Weekly averages - Savings/day: " + weeklyAvgSavings + ", Loans/day: " + weeklyAvgLoans);
            System.out.println("🔍 DEBUG: Weekly growth rate: " + weeklyGrowthRate + "%");
            
            // Monthly Report Data
            model.addAttribute("monthlySavings", monthlySavings);
            model.addAttribute("monthlyLoans", monthlyLoans);
            model.addAttribute("monthlySavingsTotal", monthlySavingsTotal);
            model.addAttribute("monthlyLoansTotal", monthlyLoansTotal);
            model.addAttribute("monthlyTransactions", monthlySavings.size() + monthlyLoans.size());
            
            // Calculate monthly statistics
            int daysInMonth = today.lengthOfMonth();
            int daysPassed = today.getDayOfMonth();
            double monthlyAvgSavings = monthlySavings.size() > 0 ? monthlySavingsTotal / daysPassed : 0;
            double monthlyAvgLoans = monthlyLoans.size() > 0 ? monthlyLoansTotal / daysPassed : 0;
            double monthlyProjectedSavings = monthlyAvgSavings * daysInMonth;
            double monthlyProjectedLoans = monthlyAvgLoans * daysInMonth;
            
            // Improved growth rate calculation
            double monthlyGrowthRate = 0;
            if (weeklySavingsTotal > 0 && monthlySavingsTotal > weeklySavingsTotal) {
                monthlyGrowthRate = ((monthlySavingsTotal - weeklySavingsTotal) / weeklySavingsTotal) * 100;
            } else if (weeklySavingsTotal > 0 && monthlySavingsTotal < weeklySavingsTotal) {
                monthlyGrowthRate = -((weeklySavingsTotal - monthlySavingsTotal) / weeklySavingsTotal) * 100;
            } else if (weeklySavingsTotal == 0 && monthlySavingsTotal > 0) {
                monthlyGrowthRate = 100; // First transactions of the month
            }
            
            System.out.println("🔍 DEBUG: Growth Rate Calculation:");
            System.out.println("🔍 DEBUG: Monthly Savings Total: " + monthlySavingsTotal);
            System.out.println("🔍 DEBUG: Weekly Savings Total: " + weeklySavingsTotal);
            System.out.println("🔍 DEBUG: Monthly - Weekly: " + (monthlySavingsTotal - weeklySavingsTotal));
            System.out.println("🔍 DEBUG: Growth Rate: " + monthlyGrowthRate + "%");
            System.out.println("🔍 DEBUG: Weekly Savings > 0: " + (weeklySavingsTotal > 0));
            
            model.addAttribute("monthlyAvgSavings", monthlyAvgSavings);
            model.addAttribute("monthlyAvgLoans", monthlyAvgLoans);
            model.addAttribute("monthlyProjectedSavings", monthlyProjectedSavings);
            model.addAttribute("monthlyProjectedLoans", monthlyProjectedLoans);
            model.addAttribute("monthlyGrowthRate", monthlyGrowthRate);
            model.addAttribute("daysInMonth", daysInMonth);
            model.addAttribute("daysPassed", daysPassed);
            
            System.out.println("🔍 DEBUG: Monthly stats - Days passed: " + daysPassed + "/" + daysInMonth);
            System.out.println("🔍 DEBUG: Monthly averages - Savings/day: " + monthlyAvgSavings + ", Loans/day: " + monthlyAvgLoans);
            System.out.println("🔍 DEBUG: Monthly projections - Savings: " + monthlyProjectedSavings + ", Loans: " + monthlyProjectedLoans);
            
            System.out.println("🔍 DEBUG: Daily - Savings: " + dailySavingsTotal + ", Loans: " + dailyLoansTotal);
            System.out.println("🔍 DEBUG: Weekly - Savings: " + weeklySavingsTotal + ", Loans: " + weeklyLoansTotal);
            System.out.println("🔍 DEBUG: Monthly - Savings: " + monthlySavingsTotal + ", Loans: " + monthlyLoansTotal);
            
            return "admin/reports";
        } catch (Exception e) {
            System.err.println("❌ ERROR: Error loading admin reports: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading reports: " + e.getMessage());
            return "admin/reports";
        }
    }
    
    @GetMapping("/logout")
    public String adminLogout() {
        return "redirect:/login";
    }
}
