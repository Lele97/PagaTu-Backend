package com.pagatu.coffee.repository;

import com.pagatu.coffee.dto.UtenteDetailDto;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    @Query("""
                select  new com.pagatu.coffee.dto.UtenteDetailDto(
                   u.id,
                        u.authId,
                        u.username,
                        u.email,
                        u.name,
                        u.lastname
                ) from Utente u where u.username = :username
            """)
    Optional<UtenteDetailDto> findUserByUsername(@Param("username") String username);

    Optional<Utente> findByUsername(String username);

    Utente findByEmail(String email);

    Optional<Utente> findByAuthId(Long authId);
}
