package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.reservation.pfeespace.entity.Role;
import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    // ================= AJOUT POUR LE CHATBOT ADMIN =================

    long countByRole(Role role);

}