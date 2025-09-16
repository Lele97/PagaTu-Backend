package com.pagatu.auth.repository;

import com.pagatu.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    Optional<User> getByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :username")
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
