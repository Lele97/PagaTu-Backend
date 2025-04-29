package com.pagatu.coffee.repository;

import com.pagatu.coffee.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findTop10ByOrderByDataPagamentoDesc();

    Optional<Pagamento> findTopByOrderByDataPagamentoDesc();

    List<Pagamento> findByUserIdOrderByDataPagamentoDesc(Long userId);
}
