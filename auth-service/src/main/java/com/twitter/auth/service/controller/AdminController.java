package com.twitter.auth.service.controller;

import com.twitter.auth.service.Model.AuditLog;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // GET ALL USERS
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    // GET USER
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    // LOCK USER
    @PutMapping("/lock/{id}")
    public String lockUser(@PathVariable Long id) {
        adminService.lockUser(id);
        return "User locked successfully";
    }

    // UNLOCK USER
    @PutMapping("/unlock/{id}")
    public String unlockUser(@PathVariable Long id) {
        adminService.unlockUser(id);
        return "User unlocked successfully";
    }

    // DISABLE USER
    @PutMapping("/disable/{id}")
    public String disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return "User disabled successfully";
    }

    // ENABLE USER
    @PutMapping("/enable/{id}")
    public String enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return "User enabled successfully";
    }

    // AUDIT LOGS
    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return adminService.getAuditLogs();
    }
}