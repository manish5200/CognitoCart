package com.manish.smartcart.controller;

import com.manish.smartcart.dto.admin.DashboardResponse;
import com.manish.smartcart.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }


    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam(defaultValue = "5") int stockThreshold,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "5")int pageSize){
        try{
            // threshold: items with stock less than this
            // page/size: pagination for the Top Sellers list
            DashboardResponse adminStats = adminService.getAdminStats(stockThreshold, pageNumber, pageSize);
            if(adminStats.getTopSellingProducts().isEmpty()){
                return  ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "There is no top selling product in the system"));
            }else{
                return ResponseEntity.status(HttpStatus.OK).body(adminStats);
            }

        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
