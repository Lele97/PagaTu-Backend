package com.pagatu.coffee.repository;

import com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoDto;
import com.pagatu.coffee.entity.Pagamento;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Payment entity operations.
 */
@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("SELECT p FROM Pagamento p WHERE p.userGroupMembership.utente = :utente ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByUtenteOrderByDataPagamentoDesc(@Param("utente") Utente utente);

    @Query("""
                SELECT new com.pagatu.coffee.dto.ClassificaPagamentiPerGruppoDto(u.username,
                    SUM(p.importo),
                    COUNT(p.id)
                )
                FROM Pagamento p
                JOIN p.userGroupMembership ugm
                JOIN ugm.group g
                JOIN ugm.utente u
                WHERE g.name = :groupName
                GROUP BY u.username
                ORDER BY SUM(p.importo) DESC
                limit 5
            """)
    List<ClassificaPagamentiPerGruppoDto> classificaPagamentiPerGruppo(@Param("groupName") String groupName);
}