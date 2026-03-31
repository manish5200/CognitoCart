package com.manish.smartcart.config;

import com.manish.smartcart.enums.AuthProvider;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.CustomerProfile;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.service.RefreshTokenService;
import com.manish.smartcart.util.JwtUtil;
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

        if(email == null){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by Google");
            return;
        }

        // Find or Create the User
        Users user = usersRepository.findByEmail(email).orElseGet(() -> {
            log.info("New Google OAuth2 User registration detected: {}", email);

            Users newUser = Users.builder()
                    .email(email)
                    .fullName(name)
                    .authProvider(AuthProvider.GOOGLE)
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
        if(user.getAuthProvider() != AuthProvider.GOOGLE){
            log.warn("User {} tried to login with Google but is registered as LOCAL.", email);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in with your password.");
            return;
        }

        // Generate Tokens locally
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        // NO FRONTEND YET: We print the tokens directly to the browser!
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonPayload = String.format(
                """
                        {
                          "message": "Google Login Successful! Copy these tokens to Swagger/Postman.\
                        ",
                          "email": "%s",
                          "accessToken": "%s",
                          "refreshToken":\
                         "%s"
                        }""",
                email, accessToken, refreshToken
        );

        response.getWriter().write(jsonPayload);
        response.getWriter().flush();
    }

}
