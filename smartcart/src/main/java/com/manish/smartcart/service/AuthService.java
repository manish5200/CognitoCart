package com.manish.smartcart.service;

import com.manish.smartcart.config.jwt.JwtUtil;
import com.manish.smartcart.dto.*;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.CustomerProfile;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtService;
    //Login
   public ResponseEntity<?>login(LoginRequest loginRequest){
         try{
             Authentication authentication = authenticationManager.authenticate(
                     new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword()));

             if(authentication.isAuthenticated()){
                  String token = jwtService.generateToken(loginRequest.getEmail());
                  String role = usersRepository.findByEmail(loginRequest.getEmail()).orElseThrow(()-> new NullPointerException("Error in finding the role.")).getRole().name();
                  return ResponseEntity.status(HttpStatus.OK).body(new LoginResponse(token,200,role));
             }else{
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "username or password incorrect"));
             }

         } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in login", e.getMessage()));
         }
    }



    public ResponseEntity<?>registerCustomer(CustomerAuthRequest customerAuthRequest){
                 try{
                     // 1. Validate email and role
                     String email = customerAuthRequest.getEmail() == null ? null : customerAuthRequest.getEmail();
                     if(email == null){
                         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email is required"));
                     }
                     if(usersRepository.existsByEmail(email)){
                         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email already exists"));
                     }

                     String roleString = customerAuthRequest.getRole();
                     Role role = roleString == null ||
                             roleString.isBlank() ? Role.CUSTOMER : Role.valueOf(roleString);

                     if(role != Role.CUSTOMER){
                         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid role"));
                     }

                     String hashedPassword = passwordEncoder.encode(customerAuthRequest.getPassword());

                     // 2. Create User
                     Users user = new Users(email,hashedPassword,role);

                     // 3. Create Customer Profile
                     CustomerProfile customerProfile = new CustomerProfile();
                     customerProfile.setName(customerAuthRequest.getName());
                     customerProfile.setPhone(customerAuthRequest.getPhone());
                     customerProfile.setDefaultShippingAddress(customerAuthRequest.getShippingAdder());
                     customerProfile.setDefaultBillingAddress(customerAuthRequest.getBillingAdder());

                     // 4. Link both sides (VERY IMPORTANT)
                     user.setCustomerProfile(customerProfile);

                     // 5. Save only user (cascade persists profile)
                     Users savedUser = usersRepository.save(user);


                     //don't expose hashed password
                     Map<String,Object> body = new LinkedHashMap<>();
                     body.put("id", customerProfile.getId());
                     body.put("name", customerProfile.getName());
                     body.put("email", savedUser.getEmail());
                     body.put("role", savedUser.getRole());
                     body.put("createdAt", savedUser.getCreatedAt());
                     body.put("phone" , customerProfile.getPhone());

                     return ResponseEntity.status(HttpStatus.OK).body(Map.of("Customer registered successfully.✅",body));

                 }catch(Exception e){
                     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in registerCustomer", e.getMessage()));
                 }
    }



    //Register Seller
    public ResponseEntity<?>registerSeller(SellerAuthRequest sellerAuthRequest){
        try{
            // 1. Validate email
            String email = sellerAuthRequest.getEmail() == null ? null : sellerAuthRequest.getEmail();
            if(email == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email is required"));
            }
            if(usersRepository.existsByEmail(email)){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email already exists"));
            }

            String roleString = sellerAuthRequest.getRole();
            Role role = roleString == null ||
                    roleString.isBlank() ? Role.SELLER : Role.valueOf(roleString);

            if(role != Role.SELLER){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid role"));
            }

            String hashedPassword = passwordEncoder.encode(sellerAuthRequest.getPassword());

            // 2. Create User
            Users user = new Users(email,hashedPassword,role);

            // 3. Create Customer Profile
            SellerProfile sellerProfile = new SellerProfile();
            sellerProfile.setStoreName(sellerAuthRequest.getStoreName());
            sellerProfile.setBusinessAddress(sellerAuthRequest.getBusinessAdder());
            sellerProfile.setGstin(sellerAuthRequest.getGstin());
            sellerProfile.setPanCard(sellerAuthRequest.getPanCard());
            // 4. Link both sides (VERY IMPORTANT)
            user.setSellerProfile(sellerProfile);

            // 5. Save only user (cascade persists profile)
            Users savedUser = usersRepository.save(user);


            //don't expose hashed password
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("id", sellerProfile.getId());
            body.put("name", sellerProfile.getStoreName());
            body.put("Business Address", sellerProfile.getBusinessAddress());
            body.put("GST IN", sellerProfile.getGstin());
            body.put("PAN CARD" , sellerProfile.getPanCard());
            body.put("email", savedUser.getEmail());
            body.put("role", savedUser.getRole());
            body.put("createdAt", savedUser.getCreatedAt());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("Seller registered successfully.✅",body));

        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in registering seller.❌", e.getMessage()));
        }
    }


}
