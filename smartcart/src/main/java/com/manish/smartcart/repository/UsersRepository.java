package com.manish.smartcart.repository;

import com.manish.smartcart.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users,Long> {

    Optional<Users>findByEmail(String email);
}
