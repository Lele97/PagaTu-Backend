package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Pagamento;
import com.pagatu.coffee.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    // Cambia questo metodo per fare riferimento al campo 'utente.id' invece che 'userId'
    List<Pagamento> findByUtente_IdOrderByDataPagamentoDesc(Long utenteId);

    // Oppure usa una query JPQL personalizzata
    @Query("SELECT p FROM Pagamento p WHERE p.utente.id = :utenteId ORDER BY p.dataPagamento DESC")
    List<Pagamento> findByUtenteIdOrderByDataPagamentoDesc(@Param("utenteId") Long utenteId);

    List<Pagamento> findTop10ByOrderByDataPagamentoDesc();

    List<Pagamento> findTop1ByOrderByDataPagamentoDesc();

    @Query("SELECT DISTINCT p.utente FROM Pagamento p")
    List<Utente> findDistinctUtente();
}