package com.manish.smartcart.service;

import com.manish.smartcart.config.jwt.JwtUtil;
import com.manish.smartcart.dto.auth.*;
import com.manish.smartcart.enums.ErrorCode;
import com.manish.smartcart.enums.Gender;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.CustomerProfile;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.util.PhoneUtil;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
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
@AllArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtService;


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


    // -----------------------------------------
    // REGISTER CUSTOMER
    // -----------------------------------------

   public ResponseEntity<?>registerCustomer(CustomerAuthRequest request){
     try{
         // 1. Validate email and role
         String email = request.getEmail() == null ? null : request.getEmail();
         if(email == null){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email is required"));
         }
         if(usersRepository.existsByEmail(email)){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email already exists"));
         }

         // 1. Create User with Hoisted Identity Fields
         Users user = new Users();
         user.setEmail(request.getEmail());
         user.setPassword(passwordEncoder.encode(request.getPassword()));
         user.setRole(Role.CUSTOMER);
         user.setFullName(request.getName()); // Hoisted

         String normalizedPhone = PhoneUtil.normalize(request.getPhone(), "+91");
         if(usersRepository.existsByPhone(normalizedPhone)){
             throw new RuntimeException(String.valueOf(ErrorCode.PHONE_ALREADY_EXISTS));
         }
         user.setPhone(normalizedPhone);
         user.setActive(true);
         user.setDateOfBirth(request.getDateOfBirth());
         user.setGender(Gender.valueOf(request.getGender()));
         // 2. Create Lean Customer Profile
         CustomerProfile profile = new CustomerProfile();
         profile.setUser(user);
         // 3. Link both sides (VERY IMPORTANT)
         user.setCustomerProfile(profile);
         // 4. Save User (Cascade persists profile)
         Users savedUser = usersRepository.save(user);

         //don't expose hashed password
         Map<String,Object> body = new LinkedHashMap<>();
         body.put("id", profile.getId());
         body.put("name", savedUser.getFullName());
         body.put("email", savedUser.getEmail());
         body.put("role", savedUser.getRole());
         body.put("createdAt", savedUser.getCreatedAt());
         body.put("phone" , savedUser.getPhone());
         return ResponseEntity.status(HttpStatus.OK).body(Map.of("Customer registered successfully.✅",body));

     }catch(Exception e){
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in registerCustomer", e.getMessage()));
     }
}



//Register Seller
public ResponseEntity<?>registerSeller(SellerAuthRequest request){
        try{
        // 1. Validate email
        String email = request.getEmail() == null ? null : request.getEmail();
        if(email == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email is required"));
        }
        if(usersRepository.existsByEmail(email)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "email already exists"));
        }

        // 2. Create User with Hoisted Identity Fields
        Users user = new Users();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.SELLER);
        user.setFullName(request.getStoreName());

        // 3. Create Seller Profile
        SellerProfile profile = new SellerProfile();
        profile.setStoreName(request.getStoreName());
        profile.setBusinessAddress(request.getBusinessAdder());
        profile.setGstin(request.getGstin());
        profile.setPanCard(request.getPanCard());

        // 4. Link both sides (VERY IMPORTANT)
        profile.setUser(user);
        user.setSellerProfile(profile);

        // 5. Save only user (cascade persists profile)
        Users savedUser = usersRepository.save(user);


        //don't expose hashed password
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("id", profile.getId());
        body.put("name", profile.getStoreName());
        body.put("Business Address", profile.getBusinessAddress());
        body.put("GST IN", profile.getGstin());
        body.put("PAN CARD" , profile.getPanCard());
        body.put("email", savedUser.getEmail());
        body.put("role", savedUser.getRole());
        body.put("createdAt", savedUser.getCreatedAt());
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("Seller registered successfully.✅",body));

        }catch(Exception e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error in registering seller.❌", e.getMessage()));
        }
        }

}
