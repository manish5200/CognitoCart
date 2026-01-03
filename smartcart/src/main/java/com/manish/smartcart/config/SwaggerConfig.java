package com.manish.smartcart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI  customOpenAPI() {
        return new OpenAPI()
                .info( new Info()
                        .title("CognitoCart API")
                        .version("1.0")
                        .description("This is the API for Cognito Cart API"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")) //Apply globally
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))); // Define JWT scheme
    }
}
