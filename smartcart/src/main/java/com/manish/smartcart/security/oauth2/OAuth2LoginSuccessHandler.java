package com.manish.smartcart.security.oauth2;

import com.manish.smartcart.shared.enums.AuthProvider;
import com.manish.smartcart.shared.enums.Role;
import com.manish.smartcart.user.model.CustomerProfile;
import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.user.repository.UsersRepository;
import com.manish.smartcart.auth.service.RefreshTokenService;
import com.manish.smartcart.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j

public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws ServletException, IOException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;

        String registrationId = authToken.getAuthorizedClientRegistrationId()
                .toUpperCase();
        AuthProvider provider = AuthProvider.valueOf(registrationId); // GOOGLE

        OAuth2User oAuth2User = authToken.getPrincipal();

        // strictly Google payload extraction for now
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        String frontendUrl = "http://localhost:4200/oauth2/callback";

        if(email == null){
            response.sendRedirect(frontendUrl + "?error=no_email");
            return;
        }

        // Find or Create the User
        Users user = usersRepository.findByEmail(email).orElseGet(() -> {
            log.info("New Google OAuth2 User registration detected: {}", email);

            Users newUser = Users.builder()
                    .email(email)
                    .fullName(name)
                    .authProvider(provider)
                    .emailVerified(true) // Google confirmed the email
                    .role(Role.CUSTOMER) // Default role -- MUST be Customer first!
                    .active(true)
                    .build();

            // Link the Customer Profile (Mandatory in your architecture)
            CustomerProfile profile = CustomerProfile.builder()
                    .user(newUser)
                    .build();
            newUser.setCustomerProfile(profile);
            return usersRepository.save(newUser);
        });

        // Prevent LOCAL users from hijacking accounts
        if(user.getAuthProvider() != provider){
            log.warn("User {} tried to login with OAuth2 but is registered as LOCAL.", email);
            response.sendRedirect(frontendUrl + "?error=local_account_exists");
            return;
        }

        // Generate Tokens locally
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        
        // Build URL carefully, URL-encoding could be added but for simple names it works.
        String redirectUrl = String.format("%s?token=%s&refresh=%s&id=%d&email=%s&name=%s&role=%s", 
                frontendUrl, 
                accessToken, 
                refreshToken,
                user.getId(),
                java.net.URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8),
                user.getRole().name()
        );
        
        response.sendRedirect(redirectUrl);
    }

}
