package com.manish.smartcart.model.user;

import com.manish.smartcart.enums.Gender;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name="users")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    @Email @NotBlank
    private String email;

    @Column(nullable = false)
    @Size(min=4)
    private String password;

    // --- PHASE 1: HOISTED IDENTITY FIELDS ---
    @NotBlank
    private String fullName;

    @Column(nullable = false, length = 20, unique = true)
    private String phone;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean active = true;

    // --- PHASE 1: LOGISTICS SHORTCUT ---
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_address_id")
    private Address primaryAddress;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerProfile customerProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerProfile sellerProfile;

    public Users(String email, String password, Role role) {
         this.email = email;
         this.password = password;
         this.role = role;
    }
}
