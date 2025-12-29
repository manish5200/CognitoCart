package com.manish.smartcart.controller;

import com.manish.smartcart.dto.admin.DashboardResponse;
import com.manish.smartcart.dto.admin.StatusChangeRequest;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
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
    private final OrderMapper orderMapper;
    public AdminController(AdminService adminService,  OrderMapper orderMapper) {
        this.adminService = adminService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam(defaultValue = "5") int stockThreshold,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "5")int pageSize){
            // threshold: items with stock less than this
            // page/size: pagination for the Top Sellers list
            DashboardResponse adminStats = adminService.getAdminStats(stockThreshold, pageNumber, pageSize);
            if(adminStats.getTopSellingProducts().isEmpty()){
                return  ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "There is no top selling product in the system"));
            }else{
                return ResponseEntity.status(HttpStatus.OK).body(adminStats);
            }
    }

    @PostMapping("/change-order-status")
    public ResponseEntity<?> changeOrderStatus(@RequestBody StatusChangeRequest request){
            Order order = adminService.changeTheStatusOfOrders(request);
            return ResponseEntity.ok(orderMapper.toOrderResponse(order));
    }
}
