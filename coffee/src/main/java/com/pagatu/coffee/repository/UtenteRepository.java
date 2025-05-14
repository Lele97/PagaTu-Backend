package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    List<Utente> findByAttivoTrue();
    Optional<Utente> findByUsername(String username);
    Optional<Utente> findByEmail(String email);
    Optional<Utente> findByAuthId(Long authId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByAuthId(Long authId);
}
