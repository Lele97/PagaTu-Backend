package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Group;
import com.pagatu.coffee.entity.Pagamento;
import com.pagatu.coffee.entity.UserGroupMembership;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    // Find payments by membership
    List<Pagamento> findByUserGroupMembershipOrderByDataPagamentoDesc(UserGroupMembership membership);
    
    // Find payments by membership with pagination
    @Query("SELECT p FROM Pagamento p JOIN FETCH p.userGroupMembership m JOIN FETCH m.utente WHERE p.userGroupMembership = :membership ORDER BY p.dataPagamento DESC")
    Page<Pagamento> findByUserGroupMembershipOrderByDataPagamentoDesc(@Param("membership") UserGroupMembership membership, Pageable pageable);

    // Find payments by user (across all groups)
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.utente = :utente ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByUtenteOrderByDataPagamentoDesc(@Param("utente") Utente utente);

    // Find payments by group
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.group = :group ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByGroupOrderByDataPagamentoDesc(@Param("group") Group group);
    
    // Find payments by group with pagination and optimized fetching
    @Query("SELECT p FROM Pagamento p JOIN FETCH p.userGroupMembership m JOIN FETCH m.utente WHERE m.group = :group ORDER BY p.dataPagamento DESC")
    Page<Pagamento> findByGroupOrderByDataPagamentoDesc(@Param("group") Group group, Pageable pageable);

    // Find payments by user and group
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.utente = :utente AND p.userGroupMembership.group = :group ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByUtenteAndGroupOrderByDataPagamentoDesc(@Param("utente") Utente utente, @Param("group") Group group);

    // Find latest payment by user in a specific group
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.utente = :utente AND p.userGroupMembership.group = :group ORDER BY p.dataPagamento DESC LIMIT 1")
    Optional<Pagamento> findLatestByUtenteAndGroup(@Param("utente") Utente utente, @Param("group") Group group);

    // Find latest payment in a group
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.group = :group ORDER BY p.dataPagamento DESC LIMIT 1")
    Optional<Pagamento> findLatestByGroup(@Param("group") Group group);

    // Find payments in a date range for a group
    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.group = :group AND p.dataPagamento BETWEEN :startDate AND :endDate ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByGroupAndDateRangeOrderByDataPagamentoDesc(
            @Param("group") Group group,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count payments by group
    @Query("SELECT COUNT(p) FROM Pagamento p WHERE p.userGroupMembership.group = :group")
    long countByGroup(@Param("group") Group group);

    // Sum of payments by group
    @Query("SELECT COALESCE(SUM(p.importo), 0) FROM Pagamento p WHERE p.userGroupMembership.group = :group")
    java.math.BigDecimal sumImportoByGroup(@Param("group") Group group);

    // Sum of payments by user in a specific group
    @Query("SELECT COALESCE(SUM(p.importo), 0) FROM Pagamento p WHERE p.userGroupMembership.utente = :utente AND p.userGroupMembership.group = :group")
    java.math.BigDecimal sumImportoByUtenteAndGroup(@Param("utente") Utente utente, @Param("group") Group group);
}